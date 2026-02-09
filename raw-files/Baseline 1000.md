# Сырые данные: Нагрузочный тест при 1000 RPS (Критический уровень)

## Результаты симуляции Gatling
При max rps = 1000
2026-02-09 18:36:23                                         220s elapsed        
---- Requests ------------------------------------------------------------------
> Global                                                   (OK=80291  KO=55263 )
> Auth Login                                               (OK=23156  KO=26630 )
> Action Request                                           (OK=52943  KO=27812 )
> Auth Logout                                              (OK=4192   KO=821   )
---- Errors --------------------------------------------------------------------
> j.n.ConnectException: connect(..) failed: Cannot assign reques  25663 (46.44%)
ted address
> Request timeout to localhost/127.0.0.1:8080 after 60000 ms      20177 (36.51%)
> status.find.is(200), but actually found 403                      8818 (15.96%)
> j.i.IOException: Premature close                                  605 ( 1.09%)

---- Baseline: Standard User Flow ----------------------------------------------
[#######----------------------------------------------------------


/mnt/c/WSL/Java/AQA_NC$ ss -s
Total: 88788
## Состояние сетевого стека (ss -s)
Демонстрация переполнения таблицы соединений и большого количества closed/timewait сокетов.
TCP:   108633 (estab 53774, closed 35725, orphaned 0, timewait 11376)

Transport Total     IP        IPv6
RAW       0         0         0        
UDP       5         4         1        
TCP       72908     55830     17078    
INET      72913     55834     17079    
FRAG      0         0         0        

/mnt/c/WSL/Java/AQA_NC$