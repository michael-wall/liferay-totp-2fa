**************************************
Intro:
**************************************

This project is an OSGI bundle implementation of 2 Factor Authentication support for Liferay DXP 7.1 using the Time-based One-Time Password algorithm (TOTP).

TOTP is an algorithm that computes a one-time password using a user specific shared secret key and the current time. TOTP is widely used in two-factor authentication systems.

The project adds an 'Authentication Code' field to the Liferay Login screen below the Password field and verifies the 'Authentication Code' entered by the user as part of the Login workflow. 

The 'Google Authenticator' or 'Authy 2-Factor Authentication' apps (available for iPhone and Android) can be used by the user to generate the one-time password.

**************************************
Notes:
**************************************

i. Supported Liferay versions: DXP 7.1
ii. 2 Factor Authentication Login must be explicitly configured and enabled after initial deployment, see 'Deployment & Setup Steps' below
iii. Securely generating, storing / retrieving and distributing the user specific secreyKey is not in scope for this project:
- The secretKey should be securely generated and securely distributed to the end user
- The user specific secretKey is currently stored in plain text in System Settings for convenient retrieval, they should be securely stored and retrieved e.g. with a custom service builder service
iv. Ensure that the phone and server time are roughly the same, if not then the generated codes may not match when the comparison is done, as the code is only valid for 30 seconds
v. If the 'Google Authenticator' authenticator code is red it means it is about to expire
- Wait until a new one is generated before trying as a time difference of a few seconds between the phone and server means it may not work
vi. Users with the Liferay Administrator role will always bypass the TOTP 2FA check on login
vii. A bug in the Liferay DXP 7.1 Senna / Single Page Application (SPA) implementation unexpectedly prevents the Login Modal Dialog form body fields being included in the Authenticator methods parameterMap
- This prevents the Authenticator functioning as expected, as the Authenticator Code field is missing
- Senna is turned off for the Login form / portlet through the login.jsp fragment

**************************************
OSGi Bundles:
**************************************

The project consists of the following OSGi bundles:

com.mw.totp-2fa.login.fragment:
- Contains a fragment to overide Login Portlet login.jsp
- The fragment disables Senna for the login portlet (by adding data-senna-off="true" to the aui:form tag)
- The fragment adds a new dynamic include with key="com.liferay.login.web#/login.jsp#loginFieldsPost" below the Password field

com.mw.totp-2fa.login.auth:
- Contains an auth.pipeline.post Authenticator that extracts and verifies the Authenticator Code from the Login form based on the users secretKey and the current time
- Contains the Dynamic Include the adds the Authenticator Code field to the Login screen

com.mw.totp-2fa.impl:
- Contains the OSGi component that uses the TOTP implementations to generate the TOTP Code using the users secretKey and the current time
- Contains the System Settings > Security > TOTO 2FA Configuration

**************************************
Deployment & Setup Steps:
**************************************

1. The Repository is a Liferay Workspace. Clone or download the Repository and import into Eclipse

2. Build the 3 OSGi bundles with gradle

3. Copy the 3 OSGi bundles to the Liferay deploy folder and confirm they deploy successfully with the Gogo shell
- totp-2fa-login-fragment is a Fragment bundle so it will remain at Resolved status

4. Login to Liferay as an Administrator / Omni Administrator (if applicable)

5. Identify the non-Administrator users you want to test with, create new users if necessary

5. Go to Control Panel > Configuration > Server Administration > Script

6. For each user, run the following Groovy script to generate a unique secreyKey, then note the secretKey generated for the specific user:

random = new java.security.SecureRandom();
byte[] bytes = new byte[20];
 random.nextBytes(bytes);
base32 = new org.apache.commons.codec.binary.Base32();
secretKey = base32.encodeToString(bytes);
out.println(secretKey);

7. Go to Control Panel > Configuration > System Settings > Security > TOTP 2FA

8. Check 'Login TOTP 2FA Enabled' and populate the 'Secret Key Mappings' field using the following syntax:
- comma separated collection of mappings where each mapping has an identifier 
(emailAddress, screenName or userId depending on company.security.auth.type in use) 
and secret key, with identifier and secretKey separated by = character. e.g. 
mw@liferay.com=C2MEXYHY62VPCNKXKAEXL5NE5MMNUEHC,wm@liferay.com=VIBLX4DXHY474BNG5YOJAHZ3KNHIKW2H

9. Click 'Save' to save and apply the changes

10. Download 'Google Authenticator' app to your phone and launch it

11. For each user click the + icon, select 'Manual Entry'. For Account enter the identifier and for Key enter the secretKey and Save

12. Logout of Liferay

13. Go to the Home page and click 'Login'. Confirm that you see the Authenticator Code field below the Password field

14. Identify which user you want to login with and Launch the 'Google Authenticator' app

15. Note the authenticator code from the phone app and login with the user credentials and the authenticator code

**************************************
TOTP Implementation
**************************************

Time-based One-Time Password algorithm (TOTP) is an algorithm that computes a one-time password using a user specific shared secret key and the current time.

TOTP is widely used in two-factor authentication systems.

See https://en.wikipedia.org/wiki/Time-based_One-time_Password_algorithm for more info on TOTP.

This project supports the following TOTP implementations:

1. https://github.com/jchambers/java-otp/ available here: https://search.maven.org/search?q=a:java-otp

2. https://github.com/j256/two-factor-auth available here: https://mvnrepository.com/artifact/com.j256.two-factor-auth/two-factor-auth

The default implementation is java-otp, this can be switched through System Settings > Security > TOTP 2FA > TOTP 2FA Implementation.