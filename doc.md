

# Hinweise

- Protokoll + dessen Abbildung auf APDUs
- Datenstrukturen erklären (definieren + …) (evtl. mit Invarianten) sowohl OnCard als auch OffCard
- Exposeüberarbeitung mit aufnehmen
- AES und kein no pad (DES, PKC verwenden)

# Hilfreiche Links

- http://www.javaworld.com/article/2076617/embedded-java/understanding-java-card-2-0.html?page=2
- http://www.binaryhexconverter.com/hex-to-binary-converter
- http://www.xorbin.com/tools/sha256-hash-calculator
- http://www.win.tue.nl/pinpasjc/docs/apis/jc222/javacard/framework/APDU.html
- http://www.ruimtools.com/doc.php?doc=jc_best
- http://des.online-domain-tools.com/?do=form-submit











# Dokumentation

## Grundlegende Beschreibung

// TODO Inhalte aus Expose übernehmen und wo nötig überarbeiten

Abschnitt 3.1. überarbeiten
Abschnitt 3.2. überarbeiten


## Schlüsselvergabe

- DES symmetrisch, CBC, NO PAD, 64 Bit Data Length, Init vector 00 00 00 00 00 00 00 00, key (plain): sosecure 
- Off-Card: bekommt über Wrapper den symmetrischen Key zur Laufzeit (nur im RAM, nicht im Source)
- On-Card: bekommt symmetrischen Key bei Produktion
- Zur Vereinfachung wird bei Smartcard hier key als Konstante deklariert (eigentlich bei Produktion in ROM geschrieben)


## Key-Phrase

Klartext: sosecure
Hex: 73 6f 73 65 63 75 72 65 (736f736563757265)


## Admin-Identity

Klartext: meister1
Hex: 6d 65 69 73 74 65 72 31 (6d65697374657231)


## Komplettverschlüsselung von Command- und Response-APDU

- Verschlüsselung zum Schutz vor APDU-Sniffing
- Angriffszenarien: vgl. https://www.blackhat.com/presentations/bh-usa-08/Buetler/BH_US_08_Buetler_SmartCard_APDU_Analysis_V1_0_2.pdf
- Nonce-Generierung zum Schutz vor Replay-Angriffen, da für jede Anfrage ein neues Nonce (eine Zufallszahl) generiert werden muss (fortwährende Änderung der übertragenen Nachrichten)

- Identitäten sind 8 Zeichen lang (bestehend aus a-z und 0-9) und werden so in Wrapper reingereicht
- Command-Nonce und Response-Nonce sind je 2 Bytes groß und zufällig generierte Werte

- Off-Card:
1. Generieren von Response-Nonce
2. Command-Nonce von Smartcard generieren lassen und abfragen (Smartcard merkt sich generiertes Command-Nonce) (bei Anfrage Responce-Nonce bereits mit übertragen)
3. <Command-Nonce><Response-Nonce><Command-APDU> verschlüsseln
4. Senden von verschlüsseltem Byte-Array

- On-Card
1. Empfangen von verschlüsseltem Byte-Array
2. Byte-Array entschlüsseln -> <Command-Nonce><Response-Nonce><Command-APDU>
3. Command-Nonce mit gespeichertem Command-Nonce vergleichen
4. Gespeichertes Command-Nonce zurücksetzen
4a. Wenn gleich, dann Command-APDU auswerten
4b. Sonst Fehler
5. Wenn Command-APDU ausgewertet, dann <Response-Nonce><Response-APDU> verschlüsseln
6. Senden von verschlüsseltem Byte Array

- Off-Card:
1. Empfangen von verschlüsseltem Byte-Array
2. Byte-Array entschlüsseln -> <Response-Nonce><Response-APDU>
3. Response-Nonce mit gespeichertem Response-Nonce vergleichen
4. Gespeichertes Response-Nonce zurücksetzen
4a. Wenn gleich, dann Response-APDU auswerten
4b. Sonst Fehler


## Wrapper-APDU-Format:

- Command-APDU: 80 <encrypted byte-array>
- Response-APDU: <encrypted byte-array>


## Encrypted Byte-Array (immer 64 Byte lang):

- Command: <Command-Nonce><Response-Nonce><Command-APDU>
- Response: <Response-Nonce><Response-APDU>


## Command-APDUs mit Response-APDUs

Weekday Bitmask: <Monday> <Tuesday> <Wednesday> <Thursday> <Friday> <Saturday> <Sunday> <ignored>
Example: 11111000(2) -> f8(16)
Current Weekday: number of weekday between 01 (Monday) .. 07 (Sunday)


### Command-Nonce generieren

INS_GET_COMMAND_NONCE (01)
- Command-Nonce generieren lassen, dass mit der nächsten Command-APDU übermittelt wird

Command-APDU: C0 01 00 00 02
Example: C001000002

Response-APDUs: 
No error: <2 bytes command nonce> <2 bytes SW_NO_ERROR>   Example: 58A09000


### Zugang basierend auf Identitäten

INS_SHOULD_OPEN (10)
- Abfragen, ob Identität an einem spezifischen (meist dem aktuellen) Wochentag Zugang hat
- keine Admin-Identität notwendig

Command-APDU: C0 10 <1 byte current weekday> 00 08 <8 bytes identity token> 01
Example: C010060008abcd1234abcd567801

Response-APDUs: 
No error: <1 byte (01 ... true, 00 ... false)> <2 bytes SW_NO_ERROR>   Example: 019000
Wrong data length: <2 bytes SW_WRONG_LENGTH>   Bytes: 6700
Invalid current weekday: <2 bytes SW_WRONG_DATA>   Bytes: 6a80


INS_GET_WEEKDAYS (50)
- Weekday-Bitmask für eine Identität abfragen
- Admin-Identität erforderlich

Command-APDU: C0 50 00 00 10 <8 bytes admin token> <8 bytes identity token> 01
Example: C0500000106d65697374657231abcd1234abcd567801

Response-APDUs:
No error: <1 byte weekday bitmask> <2 bytes SW_NO_ERROR>   Example: 069000
Not found: <2 bytes SW_RECORD_NOT_FOUND>   Bytes: 6a83
Wrong data length: <2 bytes SW_WRONG_LENGTH>   Bytes: 6700
No admin: <2 bytes SW_WRONG_DATA>   Bytes: 6a80


INS_GET_ACCESS_ITEM_AT_POS (51)
- AccessItem an einer Position in entries abfragen
- notwendig, da APDU nicht alle Einträge mit einer Response senden kann (maximale Größe wird sonst erreicht)
- wird in Verbindung mit INS_GET_MAX_SIZE verwendet
- Admin-Identität erforderlich

Command-APDU: C0 51 <2 bytes position (start 0)> 08 <8 bytes admin token> 09
Example: C0510003086d6569737465723109

Response-APDUs:
No error: <8 bytes identity token> <1 byte weekday bitmask> <2 bytes SW_NO_ERROR>   Example: abcd1234abcd5678049000
Not found or empty: <2 bytes SW_RECORD_NOT_FOUND>   Bytes: 6a83
Wrong data length: <2 bytes SW_WRONG_LENGTH>   Bytes: 6700
No admin: <2 bytes SW_WRONG_DATA>   Bytes: 6a80


INS_GET_MAX_SIZE (60)
- maximal mögliche Anzahl an Einträgen abfragen
- wird in Verbindung mit INS_GET_ACCESS_ITEM_AT_POS verwendet
- Admin-Identität erforderlich

// Command-APDU: C0 60 00 00 08 <8 bytes admin token> 02
// Example: C0600000086d6569737465723102

// Response-APDUs:
// No error: <2 bytes max size count> <2 bytes SW_NO_ERROR>   Example: 00049000
// Wrong data length: <2 bytes SW_WRONG_LENGTH>   Bytes: 6700
// No admin: <2 bytes SW_WRONG_DATA>   Bytes: 6a80

INS_PUT_ACCESS_ITEM (30)
- ein AccessItem erzeugen oder updaten
- Admin-Identität erforderlich

Command-APDU: C0 30 00 00 11 <8 bytes admin token> <8 bytes identity token> <1 byte weekday bitmask>
Example: C0300000116d65697374657231abcd1234abcd567806

Response-APDUs:
No error: <2 bytes SW_NO_ERROR>   Bytes: 9000
If full: <2 bytes SW_UNKNOWN>   Bytes: 6f00
Wrong data length: <2 bytes SW_WRONG_LENGTH>   Bytes: 6700
No admin: <2 bytes SW_WRONG_DATA>   Bytes: 6a80


INS_REMOVE_ACCESS_ITEM (40)
- ein AccessItem einer Identität entfernen
- Admin-Identität erforderlich

Command-APDU: C0 40 00 00 10 <8 bytes admin token> <8 bytes identity token>
Example: C0400000106d65697374657231abcd1234abcd5678

Response-APDUs:
No error: <2 bytes SW_NO_ERROR>   Bytes: 9000
Not found: <2 bytes SW_RECORD_NOT_FOUND>   Bytes: 6a83
Wrong data length: <2 bytes SW_WRONG_LENGTH>   Bytes: 6700
No admin: <2 bytes SW_WRONG_DATA>   Bytes: 6a80


INS_REMOVE_ALL_ACCESS_ITEMS (41)
- alle AccessItems entfernen
- Admin-Identität erforderlich

Command-APDU: C0 41 00 00 08 <8 bytes admin token>
Example: C0410000086d65697374657231

Response-APDUs:
No error: <2 bytes SW_NO_ERROR>   Bytes: 9000
Wrong data length: <2 bytes SW_WRONG_LENGTH>   Bytes: 6700
No admin: <2 bytes SW_WRONG_DATA>   Bytes: 6a80


### Globaler Zugang (unabhängig von Identitäten)

INS_SHOULD_OPEN_GLOBAL (20)
- Abfragen, ob jede Identität an einem spezifischen (meist dem aktuellen) Wochentag Zugang hat
- keine Admin-Identität notwendig

Command-APDU: C0 20 <1 byte current weekday> 00
Example: C0200600

Response-APDUs: 
No error: <1 byte (01 ... true, 00 ... false)> <2 bytes SW_NO_ERROR>   Example: 019000
Invalid current weekday: <2 bytes SW_WRONG_DATA>   Bytes: 6a80


INS_GET_GLOBAL_ACCESS (22)
- globale Weekday-Bitmask abfragen
- Admin-Identität erforderlich

Command-APDU: C0 22 00 00 08 <8 bytes admin token> 01
Example: C0220000086d6569737465723101

Response-APDUs:
No error: <1 byte weekday bitmask> <2 bytes SW_NO_ERROR>   Bytes: 9000
Wrong data length: <2 bytes SW_WRONG_LENGTH>   Bytes: 6700
No admin: <2 bytes SW_WRONG_DATA>   Bytes: 6a80


INS_SET_GLOBAL_ACCESS (21)
- globale Weekday-Bitmask setzen
- Admin-Identität erforderlich

Weekday Bitmask: <Monday> <Tuesday> <Wednesday> <Thursday> <Friday> <Saturday> <Sunday> <ignored>
Example: 11111000(2) -> f8(16)

Command-APDU: C0 21 <1 byte weekday bitmask> 00 08 <8 bytes admin token>
Example: C021f800086d65697374657231

Response-APDUs:
No error: <2 bytes SW_NO_ERROR>   Bytes: 9000
Wrong data length: <2 bytes SW_WRONG_LENGTH>   Bytes: 6700
No admin: <2 bytes SW_WRONG_DATA>   Bytes: 6a80









## Beispiel: Einfügen eines AccessItems und Benutzung von shouldOpen

### Daten

- Key-Phrase: 73 6f 73 65 63 75 72 65 (sosecure)
- Admin-Identity: 6d 65 69 73 74 65 72 31 (meister1)
- Identity: ab cd 12 34 ab cd 56 78
- Weekday Bitmask: c0 (als Bits: 1100 0000)
- Current Weekday: 02 (Tuesday)

### Applet selektieren

/atr
/select |EOSApplet

### Command-Nonce generieren lassen

Command-APDU:
C0 01 00 00 02

Mit Command- und Responce-Nonce:
0000 1111 C0 01 00 00 02

Auffüllen mit 0en auf 64 Byte:
00001111C00100000200000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000

Verschlüsseln:
5118653a46be72566788607a6c960f42a50691c7dd9818950fc1ebf96f8307357614f8666683a086fc276daa39183e336ebac57a30651fd6c93102daaa42f981

Mit Wrapper-CLA-Byte (80):
805118653a46be72566788607a6c960f42a50691c7dd9818950fc1ebf96f8307357614f8666683a086fc276daa39183e336ebac57a30651fd6c93102daaa42f981

Command-APDU senden:
/send 805118653a46be72566788607a6c960f42a50691c7dd9818950fc1ebf96f8307357614f8666683a086fc276daa39183e336ebac57a30651fd6c93102daaa42f981

Response-APDU empfangen:
78D4F00256D65254D3FE813123338BF8C26374773417CE8511BD49E6017263A69DB6E454F1FD929965220A482F96BBA239BDAC5A6448608E3713B1AB22963B35 90 00

2BDECAC8158FC68666DD84C196F54316A31F41FCA8E2E706158B23267F81D44DBDABC764A71E8CF7E75C3C513642BF9DB645C464C09584A41046EC6C6FC5179A 90 00

Wenn letzte zwei Bytes 90 00, dann gültige Antwort

Entschlüsseln der ersten 64 Bytes
2BDECAC8158FC68666DD84C196F54316A31F41FCA8E2E706158B23267F81D44DBDABC764A71E8CF7E75C3C513642BF9DB645C464C09584A41046EC6C6FC5179A
->
11 11 2b 3c 000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000

Response-Nonce prüfen (0. und 1. Byte): Entspricht 1111 wie beim Senden

Command-Nonce extrahieren (2. und 3. Byte): 2b3c

### Identity setzen

Command-APDU:
C0 30 00 00 11 6d65697374657231 abcd1234abcd5678 c0

Mit Command- und Responce-Nonce:
2b3c 2222 C0 30 00 00 11 6d65697374657231 abcd1234abcd5678 c0

Auffüllen mit 0en auf 64 Byte:
2b3c2222C0300000116d65697374657231abcd1234abcd5678c00000000000000000000000000000000000000000000000000000000000000000000000000000

Verschlüsseln:
b21b4f9ab67891ce75a6730cd1ad26dba503f6d6c72cd7fd8d9fa4ffa60ac02fb131d86c701d9cd90d115f0de7675a4ceb523cd1b96e770858f4b21951d09b2f

Mit Wrapper-CLA-Byte (80):
80b21b4f9ab67891ce75a6730cd1ad26dba503f6d6c72cd7fd8d9fa4ffa60ac02fb131d86c701d9cd90d115f0de7675a4ceb523cd1b96e770858f4b21951d09b2f

Command-APDU senden:
/send 80b21b4f9ab67891ce75a6730cd1ad26dba503f6d6c72cd7fd8d9fa4ffa60ac02fb131d86c701d9cd90d115f0de7675a4ceb523cd1b96e770858f4b21951d09b2f

Response-APDU empfangen:
469F6E105433A25B3D59D2C1A783FFCC3560E53E3D4F6A6EBCBC788162B67B9C8F2A8CC7CA3A1DF503D1E933B493A897DFC294008E1C991BAAB283ABBDC65CF7 90 00

Wenn letzte zwei Bytes 90 00, dann gültige Antwort

Entschlüsseln der ersten 64 Bytes
469F6E105433A25B3D59D2C1A783FFCC3560E53E3D4F6A6EBCBC788162B67B9C8F2A8CC7CA3A1DF503D1E933B493A897DFC294008E1C991BAAB283ABBDC65CF7
->
22 22 0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000

Response-Nonce prüfen (0. und 1. Byte): Entspricht 2222 wie beim Senden

### Neues Command-Nonce generieren lassen

- wie bereits beschrieben
- liefert bspw. Command-Nonce (zufällig generiert): 5169

### Prüfen, ob Identity Zugang hat

Command-APDU:
C0 10 02 00 08 abcd1234abcd5678 01

Mit Command- und Responce-Nonce:
5169 4444 C0 10 02 00 08 abcd1234abcd5678 01

Auffüllen mit 0en auf 64 Byte:
51694444C010020008abcd1234abcd56780100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000

Verschlüsseln:
ce27e08e8db57cacaff9f66640432e8c7b2003daa3cac69147c9ae3e17f3be2bafbeb1f8ab06cde2b6aae915551b7a1587b860671bebe588886c4bc15ac080c2

Mit Wrapper-CLA-Byte (80):
80ce27e08e8db57cacaff9f66640432e8c7b2003daa3cac69147c9ae3e17f3be2bafbeb1f8ab06cde2b6aae915551b7a1587b860671bebe588886c4bc15ac080c2

Command-APDU senden:
/send 80ce27e08e8db57cacaff9f66640432e8c7b2003daa3cac69147c9ae3e17f3be2bafbeb1f8ab06cde2b6aae915551b7a1587b860671bebe588886c4bc15ac080c2

Response-APDU empfangen:
BCBAF029B3EFE2E02CBC6A1CDBB7CD9A7A572B9351DB1D4694A2FBC4CD0B7AA114FB4C36715015F34172FDD3590055AC8FECF6994CAAEF40D358C8FA07F92CC5 90 00

Wenn letzte zwei Bytes 90 00, dann gültige Antwort

Entschlüsseln der ersten 64 Bytes
BCBAF029B3EFE2E02CBC6A1CDBB7CD9A7A572B9351DB1D4694A2FBC4CD0B7AA114FB4C36715015F34172FDD3590055AC8FECF6994CAAEF40D358C8FA07F92CC5
->
44 44 01 00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000

Response-Nonce prüfen (0. und 1. Byte): Entspricht 4444 wie beim Senden

Boolean-Byte extrahieren (2. Byte): 01 (True)

-> Identität hat nach Setzen der Erlaubnis Zugang







## On-Card

### Datenstrukturen

#### Dictionary
- Dictionary auf Basis eines Byte-Arrays (entries)
- Aufbau: <Key 1><Value 1><leerer Key><leerer Value><Key 3><Value 3>
- Maximale Anzahl an Einträgen über Konstante MAX_SIZE festlegbar
- Größe von Key bzw. Value über Konstanten festlegbar: KEY_BYTE_SIZE, VALUE_BYTE_SIZE
- lineare Suche nach Key
- Löschen eines Eintrags durch Überschreiben mit Nullen
- keine nicht-atomaren Operationen beim Schreiben in dieses Byte-Array (sonst Integrität der Daten gefährdet)

#### Temporäres Byte-Array
- cipherTemp als transientes Array (im RAM)
- für schnellen Zugriff beim Ver- und Entschlüsseln (APDU-Buffer wird vor Ver- oder Entschlüsselung in cipherTemp kopiert)
- zusätzlich nicht-atomar kopiert, da Integrität dieser temporären Daten nicht gefährdet

#### Globales AccessItem
- globalAccessItem für Bestimmung von Öffnungszeiten unabhängig von einer Identität

#### Konstanten
- phrase: der Schlüssel, um den DESKey zu erzeugen
- BLOCK_SIZE: jedes empfangene und gesendete Byte-Array besitzt diese Byte-Länge
- EMPTY_ENTRY: als Mustervergleich und zum Überschreiben von Einträgen

### API / Methoden

#### APDU-Handling

private short getCommandNonce(APDU apdu)
- übernimmt das Handling für die APDU mit INS_GET_COMMAND_NONCE

private short shouldOpen(APDU apdu)
- übernimmt das Handling für die APDU mit INS_SHOULD_OPEN

private short shouldOpenGlobal(APDU apdu)
- übernimmt das Handling für die APDU mit INS_SHOULD_OPEN_GLOBAL

private short getGlobalAccessItem(APDU apdu)
- übernimmt das Handling für die APDU mit INS_GET_GLOBAL_ACCESS

private void setGlobalAccessItem(APDU apdu)
- übernimmt das Handling für die APDU mit INS_SET_GLOBAL_ACCESS

private void putAccessItem(APDU apdu)
- übernimmt das Handling für die APDU mit INS_PUT_ACCESS_ITEM

private void removeAccessItem(APDU apdu)
- übernimmt das Handling für die APDU mit INS_REMOVE_ACCESS_ITEM

private void removeAllAccessItems(APDU apdu)
- übernimmt das Handling für die APDU mit INS_REMOVE_ALL_ACCESS_ITEMS

private short getWeekdays(APDU apdu)
- übernimmt das Handling für die APDU mit INS_GET_WEEKDAYS

private short getAccessItemAtPos(APDU apdu)
- übernimmt das Handling für die APDU mit INS_GET_ACCESS_ITEM_AT_POS

private short getMaxSize(APDU apdu)
- übernimmt das Handling für die APDU mit INS_GET_MAX_SIZE

#### Ver-/Entschlüsselung

private void decryptCommandAPDU(APDU apdu)
- entschlüsselt das übertragene Byte-Array und prüft das übertragene Command-Nonce

private void encryptResponseAPDUAndSend(APDU apdu, short outgoingLength)
- verschlüsselt die generierte APDU mit Command-Nonce und Response-Nonce und sendet die Response

#### Hilfsmethoden

private boolean isAdminIdentity(byte[] idArr, short idOff)
- überprüft, ob die gesendete Admin-Identiät gültig ist
- wird mit der intern nativ arbeitenden Methode arrayCompare umgesetzt

public short findPosition(
    	byte[] patArr, short patOff, short patLen, 
    	short entryLen, 
    	byte[] srcArr, short srcOff, short srcLen)
- Methode zum Suchen eines Mustern in einem Byte-Array
- wird für das Auffinden des Keys und von leeren Einträgen verwendet
- wenn nichts gefunden wird, ist entspricht der Rückgabewert position == (srcOff + srcLen)




## Off-Card

### Datenstrukturen

// TODO


aus Expose 3.3 (evtl. wenn nötig mit Anpassungen)


### API / Methoden




