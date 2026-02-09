# Запустить всё окружение (App + WireMock) в фоне
up:
	docker compose up --build -d

# Остановить контейнеры и удалить тома
down:
	docker compose down -v

# Базовый нагрузочный тест (default из YAML)
load-test:
	mvn gatling:test -Dgatling.simulationClass=recruitment.aqa.service.simulations.MultiSimulation

# Поиск максимума (чистый базовый сценарий)
baseline-test:
	mvn gatling:test -Dgatling.simulationClass=recruitment.aqa.service.simulations.BaselineSimulation

# Поиск предела Login Spike при фоновой нагрузке
spike-test:
	mvn gatling:test -Dgatling.simulationClass=recruitment.aqa.service.simulations.LoginSpikeSimulation

# Легкий тест (профиль light из YAML)
load-test-light:
	mvn gatling:test -Dload.profile=light

# Тяжелый тест (профиль heavy из YAML)
load-test-heavy:
	mvn gatling:test -Dload.profile=heavy

# Запустить фаззинг-тесты Jazzer
fuzz-test:
	mvn test -Dtest=CoverageGuidedFuzzingTest

# Стандартные модульные тесты
test:
	mvn test -B -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn

# Генерация отчетов (JaCoCo + Allure)
report: 
	mvn jacoco:report
	mvn allure:report
	@echo "Отчеты созданы в target/site/jacoco и target/site/allure-maven-plugin"

# Снять Thread Dump из работающего контейнера (для анализа блокировок)
# Использовать во время нагрузочного теста, когда всё зависло
thread-dump:
	@docker exec $$(docker ps -q --filter ancestor=aqa_nc-app) jstack 1 > target/thread_dump_$$(date +%H%M%S).txt
	@echo "Thread dump сохранен в target/thread_dump_*.txt"

# Мониторинг Garbage Collector (статистика в реальном времени)
# Выводит состояние памяти и время пауз каждые 2 секунды
gc-monitor:
	@echo "S0     S1     E      O      M     CCS    YGC     YGCT    FGC    FGCT    GCT"
	@docker exec $$(docker ps -q --filter ancestor=aqa_nc-app) jstat -gcutil 1 2000

# ИНСТРУКЦИЯ ПО ПРОФИЛИРОВАНИЮ:
# 1. Запустить приложение: make up
# 2. Если летит 'Cannot assign requested address', расширить порты (нужно sudo в WSL):
#    sudo sysctl -w net.ipv4.ip_local_port_range="1024 65535"
#    sudo sysctl -w net.ipv4.tcp_tw_reuse=1
# 3. Запустить тест: make baseline-test
# 4. В другом окне мониторить GC: make gc-monitor
# 5. Если всё зависло — снять Thread Dump: make thread-dump
