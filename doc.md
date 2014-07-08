

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


## AdminIdentity

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



