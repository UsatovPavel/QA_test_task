package recruitment.aqa.service.simulations

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import java.util.UUID
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import java.io.InputStream

class MultiSimulation extends Simulation {

  // YAML Config Loader
  val config: Map[String, Any] = {
    val mapper = new ObjectMapper(new YAMLFactory())
    mapper.registerModule(DefaultScalaModule)
    val profileName = sys.props.getOrElse("load.profile", "default")
    val inputStream: InputStream = getClass.getResourceAsStream("/load-config.yaml")
    if (inputStream == null) throw new RuntimeException("Could not find load-config.yaml")
    
    val fullConfig = mapper.readValue(inputStream, classOf[Map[String, Map[String, Any]]])
    val profiles = fullConfig("profiles")
    
    // Read from the 'multi' block
    val multiProfiles = profiles("multi").asInstanceOf[Map[String, Map[String, Any]]]
    multiProfiles.getOrElse(profileName, multiProfiles("default"))
  }

  def getInt(key: String, default: Int): Int = config.get(key).map(_.toString.toInt).getOrElse(default)

  val rpsWarmUp    = getInt("rps_warmup", 500)
  val rpsExtreme   = getInt("rps_extreme", 1000)
  val stormUsers   = getInt("storm_users", 2000)
  val reloginBots  = getInt("relogin_users", 500)
  val unknownBots  = getInt("unknown_users", 500)
  val ddosBots     = getInt("ddos_users", 50)
  val baseUsers    = getInt("base_users", 10)
  val duration     = getInt("duration_sec", 90).seconds

  val httpProtocol = http
    .baseUrl("http://localhost:8080")
    .acceptHeader("application/json")
    .contentTypeHeader("application/x-www-form-urlencoded")
    .header("X-API-Key", "qazWSXedc")
    .shareConnections

  def generateToken(): String = UUID.randomUUID().toString.replace("-", "").toUpperCase
  val tokenFeeder = Iterator.continually(Map("token" -> generateToken()))
  
  val fixedTokens = (1 to 100).map(_ => generateToken()).toList
  val duplicateFeeder = Iterator.continually(Map("token" -> fixedTokens(scala.util.Random.nextInt(fixedTokens.size))))

  // 1. Standard User Flow
  val scnStandardFlow = scenario("Standard User Flow")
    .feed(tokenFeeder)
    .group("Standard Session") {
      exec(http("Auth Login").post("/endpoint").formParam("token", "#{token}").formParam("action", "LOGIN").check(status.is(200)))
      .pause(1)
      .repeat(20) {
        exec(http("Action Request").post("/endpoint").formParam("token", "#{token}").formParam("action", "ACTION").check(status.is(200)))
        .pause(2.seconds, 3.seconds)
      }
      .exec(http("Auth Logout").post("/endpoint").formParam("token", "#{token}").formParam("action", "LOGOUT").check(status.is(200)))
    }

  // 2. High Volume unique login spike
  val scnUniqueLoginSpike = scenario("High Volume Unique Login")
    .feed(tokenFeeder)
    .group("Unique Login Spike") {
      exec(http("Unique Login").post("/endpoint").formParam("token", "#{token}").formParam("action", "LOGIN").check(status.is(200)))
    }

  // 3. Duplicate Login Stress
  val scnDuplicateLoginStress = scenario("Duplicate Login Stress")
    .feed(duplicateFeeder)
    .forever {
      group("Attack: Duplicate Login") {
        exec(http("Duplicate Login Attempt").post("/endpoint").formParam("token", "#{token}").formParam("action", "LOGIN")
          .check(status.in(200, 400))) 
      }
    }

  // 4. Invalid Token Request Stress
  val scnInvalidTokenStress = scenario("Invalid Token Request Stress")
    .feed(tokenFeeder)
    .forever {
      group("Attack: Invalid Tokens") {
        exec(http("Invalid Action Attempt").post("/endpoint").formParam("token", "#{token}").formParam("action", "ACTION")
          .check(status.is(400))) 
      }
    }

  // 5. Rapid Session Cycling
  val scnSessionCyclingStress = scenario("Rapid Session Cycling")
    .feed(tokenFeeder)
    .forever {
      group("Attack: Session Cycling") {
        exec(http("Fast Login").post("/endpoint").formParam("token", "#{token}").formParam("action", "LOGIN").check(status.is(200)))
        .exec(http("Fast Logout").post("/endpoint").formParam("token", "#{token}").formParam("action", "LOGOUT").check(status.is(200)))
      }
    }

  setUp(
    scnStandardFlow.inject(constantUsersPerSec(baseUsers).during(duration)),
    scnUniqueLoginSpike.inject(nothingFor(15.seconds), atOnceUsers(stormUsers)),
    scnDuplicateLoginStress.inject(nothingFor(30.seconds), atOnceUsers(reloginBots)),
    scnInvalidTokenStress.inject(nothingFor(45.seconds), atOnceUsers(unknownBots)),
    scnSessionCyclingStress.inject(nothingFor(60.seconds), atOnceUsers(ddosBots))
  ).protocols(httpProtocol)
   .throttle(
     reachRps(rpsWarmUp).in(10.seconds),
     holdFor(20.seconds),
     jumpToRps(rpsExtreme),
     holdFor(duration - 30.seconds)
   )
   .maxDuration(duration)
}
