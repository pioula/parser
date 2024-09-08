# Rozwiązanie
## Opis
Pełne rozwiązanie stawia klaster kubernetes z 1 control node i workerami na którym są:
- deployment z 3 podami API (nazwanego case12), które przyjmuje wszystkie requesty od Web Panelu
- deployment z 3 podami case3, które liczy agregacje.
- klaster redisa z 3 poshardowanymi masterami i 3 replikami
- klaster kafki składający się z 3 zookeeperów oraz 3 instancji kafki i jednego topica z partition 3, replication-factor 3
- klaster mysql z trzema instancjami i operatorem.
- dodatkowe serwisy, config mapy etc.
- local-path-storage do automatycznego tworzenia PVs

### Case 1
Requesty przychodzą do jednego z podów case12, skąd są zapisywane w klastrze redisa z kluczem jako cookie i value jako zset ograniczony do 200 elementów.
Dodatkowo każdy usertag jest wrzucany na kafkę.

### Case 2
Requesty przychodzą do jednego z podów case12 i wynik jest odczytywany z redisa.

### Case 3
Eventy tworzone w czase obsługiwania requestów z **Case 1** są konsumowane przez trzy repliki case3 i agregowane przez każde z nich w 1 minutowe buckety po pełnym kluczu.
To znaczy, że obliczana jest tylko agregacja dla zapytań typu bucket,akcja,origin,brandId,categoryId. Te agregacje są wrzucane w batchach na klaster mysql.

Gdy case12 (API) dostaje request o agregacje to wylicza ją przy pomocy SQL (czyli GROUP BY itp.)

# Stawianie klastra
Skrypt zakłada, że istnieją maszyny user@uservm1[01:10].rtb-lab.pl oraz skrypt jest uruchamiany z user@uservm110.rtb-lab.pl

aby uruchomić skrypt należy
```sh
cd setup/scripts
bash setup.sh
```
a potem podać login i hasło. W moim przypadku to będzie st115 oraz (hasło_używane_do_zalogowania_się_do_maszyn).

# Działanie
Poniżej znajduje się dashboard po 6 godzinach. Dropy występują na dokładnie samym początku działania i są spowodowane redisem. 
![Dashboard pokazuje 4368 Dropped, 181 Wrong w case 2, 6 Wrong w case 3](https://cdn.discordapp.com/attachments/1211665264492683324/1282302523339964426/image.png?ex=66dedcc9&is=66dd8b49&hm=f96040c078fe554dc8e0608a77ddc0448cb534a5e54bc4b56b92ecb19d868e69&)
Chwile później (po tych 6 godzinach) kubernetes usuwa pody case3 i próbuje je zreschedulować

Poniżej znajduje się dashboard po 7h. Dokładnie w tym momencie psuje się klaster mysql. Od tego momentu case3 ma sporadyczne Correct Answer.
![Dashboard pokazuje 4369 Dropped, 210 Wrong w case 2, 268 Wrong w case 3](https://cdn.discordapp.com/attachments/1211665264492683324/1282317405845983262/image.png?ex=66deeaa5&is=66dd9925&hm=4e629ad45480fe36ced79d358213c7b9e9cc28a32247c2e833b974bb62532f46&)

Po około 8 godzinach klaster przestaje poprawnie funkcjonować.

