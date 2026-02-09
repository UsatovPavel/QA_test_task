package recruitment.aqa.service.simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import java.util.UUID
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import java.io.InputStream

class LoginSpikeSimulation extends Simulation {

  // YAML Config Loader
  private val fullConfig: Map[String, Any] = {
    val mapper = new ObjectMapper(new YAMLFactory())
    mapper.registerModule(DefaultScalaModule)
    val inputStream: InputStream = getClass.getResourceAsStream("/load-config.yaml")
    if (inputStream == null) throw new RuntimeException("Could not find load-config.yaml")
    mapper.readValue(inputStream, classOf[Map[String, Any]])
  }

  val config: Map[String, Any] = {
    val profileName = sys.props.getOrElse("load.profile", "spike_hunt")
    val profiles = fullConfig("profiles").asInstanceOf[Map[String, Map[String, Any]]]
    profiles.getOrElse(profileName, Map.empty[String, Any])
  }

  val common: Map[String, Any] = fullConfig("common").asInstanceOf[Map[String, Any]]

  def getInt(key: String, default: Int): Int = config.get(key).map(_.toString.toInt).getOrElse(default)
  def getCommonInt(key: String, default: Int): Int = common.get(key).map(_.toString.toInt).getOrElse(default)

  val rpsBase    = getInt("rps_base", 1000)
  val spikeUsers = getInt("spike_users", 5000)
  val duration   = getInt("duration_sec", 60).seconds

  val httpProtocol = http
    .baseUrl("http://localhost:8080")
    .acceptHeader("application/json")
    .contentTypeHeader("application/x-www-form-urlencoded")
    .header("X-API-Key", "qazWSXedc")
    .shareConnections

  def generateToken(): String = UUID.randomUUID().toString.replace("-", "").toUpperCase
  val tokenFeeder = Iterator.continually(Map("token" -> generateToken()))

  val sessionRepeats = getCommonInt("session_repeats", 20)
  val pauseMin       = getCommonInt("pause_min_sec", 2).seconds
  val pauseMax       = getCommonInt("pause_max_sec", 3).seconds

  // Baseline scenario matching your 1000 RPS requirement
  val scnBase = scenario("Background Load (1000 RPS)")
    .feed(tokenFeeder)
    .exec(http("Base Login").post("/endpoint").formParam("token", "#{token}").formParam("action", "LOGIN").check(status.is(200)))
    .pause(1)
    .repeat(sessionRepeats) {
      exec(http("Base Action").post("/endpoint").formParam("token", "#{token}").formParam("action", "ACTION").check(status.is(200)))
      .pause(pauseMin, pauseMax)
    }
  
  // The Spike: target for measurement
  val scnSpike = scenario("Login Spike")
    .feed(tokenFeeder)
    .exec(http("Spike Login").post("/endpoint").formParam("token", "#{token}").formParam("action", "LOGIN").check(status.is(200)))

  setUp(
    // Steady background load
    scnBase.inject(constantUsersPerSec(rpsBase / 7).during(duration)), // divisor roughly accounts for actions/pause
    
    // The "Thundering Herd" login spike at 20 seconds
    scnSpike.inject(
      nothingFor(20.seconds),
      atOnceUsers(spikeUsers)
    )
  ).protocols(httpProtocol)
   .throttle(
      reachRps(rpsBase).in(10.seconds),
      holdFor(duration)
   )
}
