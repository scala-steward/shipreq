Usage
=====

### Start
* bin/jetty.sh start
* java -jar start.jar

### Stop
* bin/jetty.sh stop



Config
======

* Type `java -jar start.jar --list-config` and it will list at the bottom, all the enabled args.

* Config comes from:
    * start.ini
    * etc/jetty.conf (only applied when launched via bin/jetty.sh)

* As is indicated by above, config is also loaded from:
  * etc/jetty.xml
  * etc/jetty-http.xml
  * etc/jetty-deploy.xml
  * start.d/*.ini

* Additional properties files are also added to the classpath by dropping them in resources/
  This is where Prod properties, DB connection and logging config will be found.


Requirements
============
* log/
* work/


Keystore & SSL
==============

### Build instructions
* Already available should be:
    www.shipreq.com.crt
    www.shipreq.com.csr
    www.shipreq.com.key
* Combine key and crt into a pkcs12.
    openssl pkcs12 -inkey www.shipreq.com.key -in www.shipreq.com.crt -export -out www.shipreq.com.pkcs12
* Create a keystore with both the certificate and the pkcs12.
    keytool -keystore etc/keystore -import -alias shipreq_cert -file www.shipreq.com.crt
    keytool -importkeystore -srckeystore www.shipreq.com.pkcs12 -srcstoretype PKCS12 -destkeystore etc/keystore
    keytool -changealias -alias 1 -destalias shipreq_key -keystore etc/keystore

### Jetty integration
* Obfuscate passwords.
    java -cp ../jetty-9.1.0/lib/jetty-util-9.1.0.v20131115.jar org.eclipse.jetty.util.security.Password PASS1
    java -cp ../jetty-9.1.0/lib/jetty-util-9.1.0.v20131115.jar org.eclipse.jetty.util.security.Password PASS2
* Give Jetty the passwords.
    KeyStorePassword   - Keystore password.
    TrustStorePassword - Keystore password.
    KeyManagerPassword - PKCS12 password.

