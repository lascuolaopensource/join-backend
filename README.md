## OLD
fare in modo che il modulo “sos-ui-shared” sia disponibile nel package.json per sos-ui e sos-admin-ui
creare DB postgres
configurare conf/env/dev.conf o conf/env/prod.conf in sos (il backend)
compilare le due app ui (per es. con “ng build -prod -aot”)
copiare la build della ui nella cartella public_html del web server
compilare il backend (per es. con “sbt stage”)
seeding dati (con “sbt seedDataProd“ o “sbt seedDataDev”)
avviare il backend (per es. con “target/universal/stage/bin/sos -Dconfig.resource=env/prod.conf -Dhttp.port=80 -Dlogger.resource=logback.xml”)

## NEW

### Dependendcies

In order to compile the backend application you will need `Postgresql`, `sbt` and `JDK`.

#### Install Sbt

    echo "deb https://dl.bintray.com/sbt/debian /" | sudo tee -a /etc/apt/sources.list.d/sbt.list
    curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | sudo apt-key add
    sudo apt-get update
    sudo apt-get install sbt

####  Install JDK

`sudo apt install openjdk-8-jdk`

#### Install Postgresql

`sudo apt install postgresql`

`sudo systemctl start postgresql@11-main`

You need to create an user on the database in order to connect it to your application.
Set 'username' and password when prompted

    sudo -su postgres
    createuser -s -d -P username

Check connection availability and port

`pg_isready`

Then create the database defining the name on `dbname` (eg. gestionale).

`createdb dbname`

Then exit postgres user.

`exit`


### Configure

Set variables according to your environment into `conf/env/`.

Production:

`sudo nano sos-master/conf/env/prod.conf`

Development:

`sudo nano sos-master/conf/env/dev.conf`


1. `${?APPLICATION_SECRET}` is your production key secret.
2. `${JDBC_DATABASE_URL}` is yout postrges url `jdbc:postgresql://host/database` (eg. `jdbc:postgresql://localhost/gestionale`).
3. `${JDBC_DATABASE_USER}` and `${JDBC_DATABASE_PASSWORD}` are your database user credetials (eg. `postgres` and `1234`).
4. `${?ADMIN_URL}` is your admin ui domain (eg. `"http://your-frontend-domain.xyz/admin"` or `"http://dmin.your-frontend-domain.xyz"`).
5. `${?USER_URL}` is your public ui domain (eg. `"http://your-frontend-domain.xyz/"` or `"http://join.your-frontend-domain.xyz"`).
6. `${?USER_WELCOME_URL}` is your welcome domain (eg. `"http://your-frontend-domain.xyz/welcome"`).
7. `${AWS_ACCESS_KEY_ID}` and `${AWS_SECRET_KEY}` are your AWS Credentials.
8. `${?DUMMY_USER_EMAIL}` is the dummy user email address (eg. `"dummy@example.com"`).
9. `${?DUMMY_USER_PWD}` is the dummy user password.
10. `${?BRAINTREE_MERCHANT_ID}`, `${?BRAINTREE_PUBLIC_KEY}`, `${?BRAINTREE_PRIVATE_KEY}`, `${?BRAINTREE_ENVIRONMENT}` are your Braintree credentials.
11. `${?MAILER_HOST}`, `${?MAILER_USER}`, `${?MAILER_PASS}` are your email host credentials.


Watch out for http or https while setting URLs.

### Build

First get eveything needed for sbt.

`sbt`

Then you can build

`sbt stage`

You need to seed the database according with your environment.

Production:

`sbt seedDataProd`

Development:

`sbt seedDataDev`



### Launch

Launch the application

`sudo target/universal/stage/bin/sos -Dconfig.resource=env/dev.conf -Dhttp.port=9000 -Dlogger.resource=logback.xml -Dplay.evolutions.db.default.autoApply=true -Dplay.http.secret.key='QCY?tAnfk?aZ?iwrNwnxIlR6CTf:G3gf:90Latabg@5241ABR5W:1uDFN];Ik@n'`
