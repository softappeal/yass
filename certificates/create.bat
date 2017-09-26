@echo off

set keytool="keytool"

%keytool% -keystore Server.key.pkcs12    -storepass StorePass -alias key -genkeypair -keyalg RSA -validity 20000 -dname "cn=Server"
%keytool% -keystore ClientCA.key.pkcs12  -storepass StorePass -alias key -genkeypair -keyalg RSA -validity 20000 -dname "cn=ClientCA"
%keytool% -keystore Client.key.pkcs12    -storepass StorePass -alias key -genkeypair -keyalg RSA -validity 20000 -dname "cn=Client"

%keytool% -keystore Server.key.pkcs12    -storepass StorePass -alias key -exportcert -rfc -file Server.cert.pem
%keytool% -keystore ClientCA.key.pkcs12  -storepass StorePass -alias key -exportcert -rfc -file ClientCA.cert.pem

%keytool% -keystore Server.cert.pkcs12   -storepass StorePass -alias cert -importcert -noprompt -file Server.cert.pem
%keytool% -keystore ClientCA.cert.pkcs12 -storepass StorePass -alias cert -importcert -noprompt -file ClientCA.cert.pem

%keytool% -keystore Client.key.pkcs12    -storepass StorePass -alias ca -importcert -noprompt -file ClientCA.cert.pem
%keytool% -keystore Client.key.pkcs12    -storepass StorePass -alias key -certreq | %keytool% -keystore ClientCA.key.pkcs12 -storepass StorePass -alias key -gencert -validity 20000 | %keytool% -keystore Client.key.pkcs12 -storepass StorePass -alias key -importcert

set openssl="C:\Program Files\Git\mingw64\bin\openssl"
%openssl% pkcs12 -in Server.key.pkcs12 -passin pass:StorePass -out Server.key.pem -passout pass:StorePass
%openssl% pkcs12 -in Client.key.pkcs12 -passin pass:StorePass -out Client.key.pem -passout pass:StorePass

%keytool% -keystore Server.cert.pkcs12   -storepass StorePass -list
%keytool% -keystore ClientCA.cert.pkcs12 -storepass StorePass -list
%keytool% -keystore Server.key.pkcs12    -storepass StorePass -list
%keytool% -keystore ClientCA.key.pkcs12  -storepass StorePass -list
%keytool% -keystore Client.key.pkcs12    -storepass StorePass -list
