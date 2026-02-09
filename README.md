# AQA Testing Suite

Проект содержит набор автоматизированных тестов и технический аудит производительности веб-приложения.

## 🚀 Стек технологий
*   **Язык**: Java 17, Scala (для Gatling)
*   **Сборка**: Maven
*   **Тестирование**: JUnit 5, jqwik (Property-based), Jazzer (Fuzzing)
*   **Нагрузка**: Gatling
*   **Моки**: WireMock
*   **Отчетность**: Allure, Gatling Reports, JaCoCo (Coverage)
*   **Инфраструктура**: Docker Compose

---

## 🛠 Быстрый запуск

В корне проекта подготовлен `Makefile` для автоматизации развертывания и тестирования.

### 1. Подготовка окружения
Запуск приложения и WireMock в Docker-контейнерах:
```bash
make up
```

### 2. Запуск функциональных тестов (Unit + Property-based)
```bash
make test
```
Генерация Allure-отчета:
```bash
make report
```

### 3. Запуск нагрузочных тестов (Gatling)
*   **Базовый сценарий** (MultiSimulation — комбинированная нагрузка):
    ```bash
    make load-test
    ```
*   **Baseline сценарий** (постепенный рост нагрузки):
    ```bash
    make baseline-test
    ```
*   **Тест на всплеск** (Login Spike):
    ```bash
    make spike-test
    ```

---

## 📊 Анализ результатов

Главный отчет по итогам тестирования находится в файле **[SUMMARY.md](SUMMARY.md)**. Там собраны:
*   Критические дефекты, найденные фаззингом.
*   Анализ производительности и "узких мест" системы.
*   Рекомендации по оптимизации архитектуры.

### Интерактивные отчеты
После прогона тестов подробные отчеты доступны по путям:
*   **Allure**: `target/site/allure-maven-plugin/index.html`
*   **Покрытие кода (JaCoCo)**: `target/site/jacoco/index.html`
*   **Нагрузка (Gatling)**: `target/gatling/[simulation-name]/index.html`

---

## 🔍 Инструменты диагностики нагрузочного тестирования
Если приложение зависает под нагрузкой, используйте команды:
*   `make gc-monitor` — мониторинг памяти и пауз Garbage Collector.
*   `make thread-dump` — снятие дампа потоков для поиска Deadlocks.
*    `ss -s` 