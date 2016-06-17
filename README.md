# beanstalk-template
Web Service endpoint for deployment to Amazon Web Services (AWS) Beanstalk (somewhere between IAAS and PAAS).

The web app is available via HTTPS only (self signed certificate) at a custom domain and requires Client Certificate Authentication.

Note that this is a good use case for using AWS Lambda but as not available in Australia so will assess later.

##Architecture in detail
* AWS Elastic Load Balancer (ELB) is configured with an SSL certificate (self-signed) and routes requests to our application in Elastic Beanstalk (EB). 
* The application in EB comprises a Java servlet webapp that is deployed by EB to a group of 1 to N autoscaled EC2 instances running Linux and Tomcat 8.


##Features
* ELB offers load balancing and auto-scaling 
* Multi-zone deployment (tick a box in configuration) 
* Instances automatically rebuilt on major failure
* OS/Java/Tomcat updates can be applied with rolling update (or backed out) by click of a button (or api call)
* Logs can be rolled over into S3

##Security
Security options trialled were

* Pre-emptive Basic Authentication over SSL/TLS
* Client Certificates

Options not implemented due to concerns over the complexity for constructing the client requests were

* OAuth 1.0, 2.0
* Amazon Signature 4 

### Certificate based authentication
This has been proven to work with a single instance (which does not use the load balancer). Using the load balancer is harder because we need to configure security groups and TCP forwarding of SSL traffic and terminate the SSL traffic at the web server rather than the load balancer. This may be trialled later but there is no pressing requirement.

A YAML script folder `.ebextensions` is placed at the root of the deployed WAR file and is used to customise Apache (also part of Java Tomcat beanstalk image) to handle SSL.  In a contained [YAML script](src/main/webapp/.ebextensions/run.config) Apache is configured to require client certificates for all SSL traffic and a `ca.crt` file is deployed to the filesystem that Apache uses to verify client certificates (which must have been signed by the `ca.crt`, also know as a Root Certificate).

Certificate creation is fully scripted [here](update-certificates.sh).

#### Create certificate for Apache
```bash
## create certificate for Apache
cd src/main/webapp/.ebextensions/<MODE>
openssl req -x509 -nodes -days 365 -newkey rsa:2048 -keyout mysitename.key -out server.pem -keyout server-private-key.pem -subj '/CN=beanstalk-template-dev.ap-southeast-2.elasticbeanstalk.com/O=Your org/C=AU/ST=Your state/OU=Your org unit/emailAddress=youremail@gmail.com/L=Your location'
```

### Preemptive Basic authentication
This method has been proven to work including with the auto scaling load balancer.

If using Preemptive Basic Authentication then each request must have Base64 encoded authentication details in the `Authorization` HTTP header. When a request is recieved the following authentication process occurs:

* The username and password are extracted from the header
* the expected hash is retrieved from S3 using the username as a key
* the salt used for the username is retrieved from S3 using the username as a key
* the password and salt are concatenated and hashed using a hash key read from `application.properties` on classpath
* the resulting hash is compared to the expected hash and if equal the user is authenticated

#### Setting up SSL with a self-signed certificate

Note that the second `openssl` asks for a `Common Name` at which point you enter the domain for the certificate (in this case `beanstalktemplate.ap-southeast-2.elasticbeanstalk.com`).

```bash
## create certificate for Load Balancer
cd target

openssl genrsa -out my-private-key.pem 2048
openssl req -sha256 -new -key my-private-key.pem -out csr.pem
openssl x509 -req -days 3650 -in csr.pem \
  -signkey my-private-key.pem \
  -out my-certificate.pem

## deploy certificate (for load balanced auto scaled basic auth version)
sudo apt-get install awscli
aws configure
aws iam upload-server-certificate \
  --server-certificate-name beanstalkdemo \
  --certificate-body file://my-certificate.pem \
  --private-key file://my-private-key.pem.rsa
```
Then go to `AWS - Elastic Beanstalk - <your app> - Configuration - Network Tier - Load Balancing` and
* set *Listener port* to OFF, *Secure listener port* to 443
* set *SSL Certificate ID* to your newly deployed certificate (from the dropdown)

## Using a custom domain name
If you create the domain name with *AWS Route 53* then all you need to do is go to `AWS - Route 53 - Registered domains - Manage DNS - Create Record Set` and 
* enter a name 
* set *Alias* to *Yes*
* select your load balancer in *Alias Target*

Wait 10 mins or so and it will start working.

##Logging
Log file rotation to S3 can be enabled. See [here](http://docs.aws.amazon.com/elasticbeanstalk/latest/dg/using-features.logging.html).

##Testing authentication with client certificates
The `curl` command can be used to test connection using a client certificate:

Run this command from the base directory of the project (assumes you have the `client-dev.key` file in your home directory).

```bash
curl --cacert src/main/webapp/.ebextensions/certificates/dev/server.pem --cert-type pem --cert src/main/client-certificates/dev/client.crt --key ~/client-dev.key --data-urlencode "a=hello" https://beanstalk-template-dev.ap-southeast-2.elasticbeanstalk.com/submit```


Testing
----------
To run the webapp locally using jetty (and communicating with AWS):

```bash
export AWS_ACCESS_KEY=<THE_KEY>
export AWS_SECRET_ACCESS_KEY=<SECRET>
mvn jetty:run
```
This will communicate with *dev* AWS resources (unless you modify the pom otherwise).

Preparation
------------------
* Create a new beanstalk application using the AWS web console
* Environment Type - Predefined Configuration - Tomcat (don't choose Java!)
* Environment name - your-app-name-dev
* Configuration Details - Instance type - t2.nano
* Create
* On Github fork this project
* Clone this project locally
* In the pom.xml
  * rename `groupId` to your groupId
  * rename `artifactId` to *your-application-name*
  * rename `application.name` under `properties` to *your-application-name*
  * rename `application.region` to the AWS region you are going to use
* go to command line to the `beanstalk-template` application directory (rename it if you wish)
* Update details in `update-certificates.sh` (certificate details, endpoint domain name)
* Run `./update-certificates.sh`
* Copy `target/keys.tar.gz` somewhere safe
* Open `keys.tar.gz` and copy the contents of `server-private-key.pem` to the clipboard
* Run `mvn -ep "<PASTE HERE>"`
* Copy the output and add to your `~/.m2/settings.xml`:
```xml
<server>
    <server>
        <id>your-app-name-server-private-key-dev</id>
        <password>PASTE_OUTPUT_HERE</password>
    </server>
</servers>
```
* Update the pom.xml so is consuming your serverId from above
* Update the pom.xml with proxy.host, proxy.port info (leave blank if don't apply)
* Test that decrypt works:
```mvn clean install```
* Set environment variables: 
```bash
export AWS_ACCESS_KEY=<YOUR_ACCESS_KEY>
export AWS_SECRET_ACCESS_KEY=<YOUR_SECRET_KEY>
``` 
* Deploy app to beanstalk:
```bash
 mvn package aws:deploy -Dmode=dev
```
* Check dashboard on AWS Console - Beanstalk to see deployment happening
* Once healthy, visit http://your-app-name-dev.ap-southeast-2.elasticbeanstalk.com to see if port 80 site working
* To test client certificate authenticated https, go to Chrome:
 ```
 Settings - Advanced - HTTPS/SSL - Manage certificates - Your certificates - Import
 ```
 Select the `client.p12` file and enter the password from `client-password.txt`
 
* visit https://your-app-name-dev.ap-southeast-2.elasticbeanstalk.com to see if https works (port 443) (select your certificate when prompted)

Building and deploying
-----------------------

```bash
export AWS_ACCESS_KEY=<THE_KEY>
export AWS_SECRET_ACCESS_KEY=<SECRET>
## if using basic auth then 
export PASSWORD_HASH_KEY=<PASSWORD>

mvn package aws:deploy -Dmode=dev
```

Note that you must have the appropriate modal `server-private-key.pem` contents encrypted in your maven `settings.xml`. See comment in the configuration of *decrypt-maven-plugin* in [pom.xml](pom.xml) for details. 

To tag a release:
```
./tag-release.sh <VERSION>
```

To tag a release and deploy to aws in once command:
```
./tag-release-and-deploy.sh <VERSION> <MODE>
```
