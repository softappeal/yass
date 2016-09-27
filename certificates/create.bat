@echo off

set keytool_raw="C:\Program Files\Java\jdk1.8.0_102\bin\keytool"
set openssl="C:\Program Files\Git\mingw64\bin\openssl"

set keytool=%keytool_raw% -keystore
set pwd=-storepass StorePass -keypass KeyPass -alias

%keytool% Server.key.jks   %pwd% key -genkeypair -keyalg RSA -validity 20000 -dname "cn=Server"
%keytool% ClientCA.key.jks %pwd% key -genkeypair -keyalg RSA -validity 20000 -dname "cn=ClientCA"
%keytool% Client.key.jks   %pwd% key -genkeypair -keyalg RSA -validity 20000 -dname "cn=Client"

%keytool% Server.key.jks   %pwd% key -exportcert -rfc -file Server.cert.pem
%keytool% ClientCA.key.jks %pwd% key -exportcert -rfc -file ClientCA.cert.pem

%keytool% Server.cert.jks   %pwd% cert -importcert -noprompt -file Server.cert.pem
%keytool% ClientCA.cert.jks %pwd% cert -importcert -noprompt -file ClientCA.cert.pem

%keytool% Client.key.jks %pwd% ca -importcert -noprompt -file ClientCA.cert.pem
%keytool% Client.key.jks %pwd% key -certreq | %keytool% ClientCA.key.jks %pwd% key -gencert -validity 20000 | %keytool% Client.key.jks %pwd% key -importcert

%keytool_raw% -importkeystore -srckeystore Server.key.jks -destkeystore Server.key.pkcs12 -deststoretype PKCS12 -srcalias key -srcstorepass StorePass -srckeypass KeyPass -deststorepass KeyPass -destkeypass KeyPass -noprompt
%keytool_raw% -importkeystore -srckeystore Client.key.jks -destkeystore Client.key.pkcs12 -deststoretype PKCS12 -srcalias key -srcstorepass StorePass -srckeypass KeyPass -deststorepass KeyPass -destkeypass KeyPass -noprompt

%openssl% pkcs12 -in Server.key.pkcs12 -passin pass:KeyPass -out Server.key.pem -passout pass:KeyPass
%openssl% pkcs12 -in Client.key.pkcs12 -passin pass:KeyPass -out Client.key.pem -passout pass:KeyPass

%keytool% Server.cert.jks   -storepass StorePass -list -v
%keytool% ClientCA.cert.jks -storepass StorePass -list -v
%keytool% Client.key.jks    -storepass StorePass -list -v
