# Сырые данные: Примеры фаззинга и ошибок валидации

## Кейс 1: Пустая строка в параметре action
Приводит к необработанному исключению на сервере.
Фаззинг Action (ASCII): Неверное значение enum должно возвращать 400 110 ==> Ввод: ''
Overview History Retries
Status expected:<400> but was:<500>
Categories: Product defects
Severity: normal
Duration:  8ms
Execution
Test body
Тестируем некорректный ASCII action: []0s
Status expected:<400> but was:<500>


## Кейс 2: Строка 'null' как значение параметра
Сервер воспринимает "null" как строку, но валидация или логика преобразования в Enum падает.
Фаззинг Action (ASCII): Неверное значение enum должно возвращать 400 116 ==> Ввод: 'null'
Overview History Retries
Status expected:<400> but was:<500>
Categories: Product defects
Severity: normal
Duration:  8ms
Execution
Test body
Тестируем некорректный ASCII action: [null]0s
Status expected:<400> but was:<500>

## Кейс 3: Случайная ASCII строка (Action)
Любая строка, не входящая в Enum Action, вызывает 500 ошибку вместо 400.
Фаззинг Action (ASCII): Неверное значение enum должно возвращать 400 138 ==> Ввод: 'uhOYeiiDSgUuNXURIHB'
Overview History Retries
Status expected:<400> but was:<500>
java.lang.AssertionError: Status expected:<400> but was:<500>
	at org.springframework.test.util.AssertionErrors.fail(AssertionErrors.java:61)
	at org.springframework.test.util.AssertionErrors.assertEquals(AssertionErrors.java:128)
	at org.springframework.test.web.servlet.result.StatusResultMatchers.lambda$matcher$9(StatusResultMatchers.java:640)
	at org.springframework.test.web.servlet.MockMvc$1.andExpect(MockMvc.java:214)
	at recruitment.aqa.service.fuzzing.FuzzingTest.fuzzAction_Ascii_ShouldReturnBadRequest(FuzzingTest.java:111)
	at java.base/java.lang.reflect.Method.invoke(Method.java:569)
	at java.base/java.util.Optional.i


 Фаззинг Action (ASCII): Неверное значение enum должно возвращать 400 15 ==> Ввод: 'OGHQgUlHThZtGpDXdxNNdMRutwHEcWyWUNlKTnzbFtwWSLIiWIVwChGnEhesYSdmfrnivbjYNUshBKIhepAQPfoHBpZbIxivXkIDdSAyzLvMuHkGhFOuoKJlztPpYXgNCUoKWPOkfFCsqgEXwpZOCDBqQmNpsmIlLWqqpPtazUVsaHQLjksie'




## Другое
 
## Кейс 4: Пустой ввод (Jazzer refined)
Автоматизированный фаззинг покрытия также подтвердил падение на пустых данных.
 fuzzRefined(FuzzedDataProvider) <empty input>
Overview History Retries
java.lang.AssertionError: Range for response status value 500 expected:<CLIENT_ERROR> but was:<SERVER_ERROR>
Tags: jazzer
Categories: Test defects
Severity: normal

 fuzzTokenAndAction(FuzzedDataProvider) <empty input>
Overview History Retries
java.lang.AssertionError: Fuzzing found 5xx Server Error: 500
com.code_intelligence.jazzer.junit.FuzzTestFindingException: java.lang.AssertionError: Fuzzing found 5xx Server Error: 500
	at com.code_intelligence.jazzer.junit.FuzzTestExtensions.runWithHooks(FuzzTestExtensions.java:119)
	at com.code_intelligence.jazzer.junit.FuzzTestExtensions.interceptTestTemplateMethod(FuzzTestExtensions.java:78)
	at java.base/java.util.Optional.ifPresent(Optional.java:178)
	at java.base/java.util.stream.ForEachOps$ForEachOp$OfRef.accept(ForEachOps.java:183)
	at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:197)
	at java.base/java.util.stream.ForEachOps$ForEachOp$OfRef.accept(ForEachOps.java:183)
	at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:197)
	at java.base/java.util.stream.Streams$StreamBuilderImpl.forEachRemaining(Streams.java:411)
	at java.base/java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:734)
	at java.base/java.util.strea