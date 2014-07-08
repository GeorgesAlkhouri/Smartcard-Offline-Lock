

# Dokumentation

- Protokoll + dessen Abbildung auf APDUs
- Datenstrukturen erklären (definieren + …) (evtl. mit Invarianten) sowohl OnCard als auch OffCard
- Exposeüberarbeitung mit aufnehmen
- AES und kein no pad (DES, PKC verwenden)


## Schlüsselvergabe

- DES symmetrisch, CBC, NO PAD, 64 Bit Data Length, Init vector 00 00 00 00 00 00 00 00, key (plain): sosecure 
- Off-Card: bekommt über Wrapper den symmetrischen Key zur Laufzeit (nur im RAM, nicht im Source)
- On-Card: bekommt symmetrischen Key bei Produktion
- Zur Vereinfachung wird bei Smartcard hier key als Konstante deklariert (eigentlich bei Produktion in ROM geschrieben)


## Komplettverschlüsselung von Command- und Response-APDU

- Identitäten sind 8 Zeichen lang (bestehend aus a-z und 0-9) und werden so in Wrapper reingereicht
- Command-Nonce und Response-Nonce sind je 2 Bytes groß und zufällig generierte Werte

- Off-Card:
1. Command-Nonce von Smartcard generieren lassen und abfragen (Smartcard merkt sich generiertes Command-Nonce)
2. Generieren von Response-Nonce
3. <Command-Nonce><Response-Nonce><Command-APDU> verschlüsseln
4. Senden von verschlüsseltem Byte-Array

- On-Card
1. Empfangen von verschlüsseltem Byte-Array
2. Byte-Array entschlüsseln -> <Command-Nonce><Response-Nonce><Command-APDU>
3. Command-Nonce mit gespeichertem Command-Nonce vergleichen
4. Gespeichertes Command-Nonce zurücksetzen
4a. Wenn gleich, dann Command-APDU auswerten
4b. Sonst Fehler
5. Wenn ausgewertet, dann <Response-Nonce><Response-APDU> verschlüsseln
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

- …


## Key-Phrase

sosecure
73 6f 73 65 63 75 72 65
736f736563757265


## Admin-Identity

meister1
6d 65 69 73 74 65 72 31
6d65697374657231



## Hilfreiche Links

- http://www.javaworld.com/article/2076617/embedded-java/understanding-java-card-2-0.html?page=2
- http://www.binaryhexconverter.com/hex-to-binary-converter
- http://www.xorbin.com/tools/sha256-hash-calculator
- http://www.win.tue.nl/pinpasjc/docs/apis/jc222/javacard/framework/APDU.html
- http://www.ruimtools.com/doc.php?doc=jc_best
- http://des.online-domain-tools.com/?do=form-submit






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
C0 01 00 00 01

Mit Command- und Responce-Nonce:
0000 1111 C0 01 00 00 01

Auffüllen mit 0en auf 64 Byte:
00001111C00100000100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000

Verschlüsseln:
5118653a46be7256c84e37ba4ebfd3fad37d0cdd9ff225b964a29dac101913b79cc1f6033944911b14c2959122d146d9a0284aab0bdd78b8a987734ea9ba39fd

Mit Wrapper-CLA-Byte (80):
805118653a46be7256c84e37ba4ebfd3fad37d0cdd9ff225b964a29dac101913b79cc1f6033944911b14c2959122d146d9a0284aab0bdd78b8a987734ea9ba39fd

Command-APDU senden:
/send 805118653a46be7256c84e37ba4ebfd3fad37d0cdd9ff225b964a29dac101913b79cc1f6033944911b14c2959122d146d9a0284aab0bdd78b8a987734ea9ba39fd

Response-APDU empfangen:
78D4F00256D65254D3FE813123338BF8C26374773417CE8511BD49E6017263A69DB6E454F1FD929965220A482F96BBA239BDAC5A6448608E3713B1AB22963B35 90 00

Wenn letzte zwei Bytes 90 00, dann gültige Antwort

Entschlüsseln der ersten 64 Bytes
78D4F00256D65254D3FE813123338BF8C26374773417CE8511BD49E6017263A69DB6E454F1FD929965220A482F96BBA239BDAC5A6448608E3713B1AB22963B35
->
11 11 51 24 000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000

Response-Nonce prüfen (0. und 1. Byte): Entspicht 1111 wie beim Senden

Command-Nonce extrahieren (2. und 3. Byte): 5124

### Identity setzen

Command-APDU:
C0 30 00 00 11 6d65697374657231 abcd1234abcd5678 c0

Mit Command- und Responce-Nonce:
5124 2222 C0 30 00 00 11 6d65697374657231 abcd1234abcd5678 c0

Auffüllen mit 0en auf 64 Byte:
51242222C0300000116d65697374657231abcd1234abcd5678c00000000000000000000000000000000000000000000000000000000000000000000000000000

Verschlüsseln:
0e6d6dc65cc2522955c7181280f806051d6d0565a9c7343a464f4d7a4544f4130f94fcc075c319a5883bd600d170dc30c4c0bcf22f0ef8555120413328e3bc66

Mit Wrapper-CLA-Byte (80):
800e6d6dc65cc2522955c7181280f806051d6d0565a9c7343a464f4d7a4544f4130f94fcc075c319a5883bd600d170dc30c4c0bcf22f0ef8555120413328e3bc66

Command-APDU senden:
/send 800e6d6dc65cc2522955c7181280f806051d6d0565a9c7343a464f4d7a4544f4130f94fcc075c319a5883bd600d170dc30c4c0bcf22f0ef8555120413328e3bc66

Response-APDU empfangen:
469F6E105433A25B3D59D2C1A783FFCC3560E53E3D4F6A6EBCBC788162B67B9C8F2A8CC7CA3A1DF503D1E933B493A897DFC294008E1C991BAAB283ABBDC65CF7 90 00

Wenn letzte zwei Bytes 90 00, dann gültige Antwort

Entschlüsseln der ersten 64 Bytes
469F6E105433A25B3D59D2C1A783FFCC3560E53E3D4F6A6EBCBC788162B67B9C8F2A8CC7CA3A1DF503D1E933B493A897DFC294008E1C991BAAB283ABBDC65CF7
->
22 22 0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000

Response-Nonce prüfen (0. und 1. Byte): Entspicht 2222 wie beim Senden

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

Response-Nonce prüfen (0. und 1. Byte): Entspicht 4444 wie beim Senden

Boolean-Byte extrahieren (2. Byte): 01 (True)

-> Identität hat nach Setzen der Erlaubnis Zugang


