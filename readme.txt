This is an initial POC for Liferay TOTP 2FA support. See https://github.com/michael-wall/liferay-2fa for latest version


**************************************
Intro
**************************************

This project is a Proof Of Content OSGI implementation of 2 Factor Authentication in for Liferay DXP 7.1 using the Time-based One-Time Password algorithm (TOTP).

TOTP is an algorithm that computes a one-time password using a user specific shared secret key and the current time. TOTP is widely used in two-factor authentication systems.

The project adds an 'Authentication Code' field to the Liferay Login screen below the Password field and verifies the 'Authentication Code' entered by the user as part of the Login workflow. 

The Google Authenticator app (available for iPhone and Android) can be used by the user to generate the one-time password. Other 2FA apps can be used in place of the Google Authenticator app if required.

The user specific secretKeys are stored unsecured in System Settings for convenient retrieval.

**************************************
Deployment & Setup
**************************************

The following steps cover building, deploying, configuring and testing the project:

1. The GitHub Repository is a Liferay Workspace. Clone or download the Repository and import into an Eclipse Workspace as a Liferay Workspace

2. Perform a Gradle > Refresh Gradle Project then build the 3 OSGi bundles with Gradle through the Gradle Tasks view

3. Copy the 3 OSGi bundles to the Liferay deploy folder and confirm they deploy successfully with the Gogo shell
- totp-2fa-login-fragment is a Fragment bundle so it will remain at Resolved status

4. Login to Liferay as an Administrator / Omni Administrator (if applicable)

5. Make a note of all the non-Administrator users you want to test with, create any new users if necessary

6. Go to Control Panel > Configuration > Server Administration > Script

7. Copy and paste the following Groovy script into the Script field and update the userIdentifiers array values to match the users from step 5, using emailAddress, screenName or userId (as String) based on company.security.auth.type in use:

String[] userIdentifiers = ["mw@liferay.com", "wm@liferay.com"];
for (i = 0; i < userIdentifiers.length; i++) {
random = new java.security.SecureRandom();
byte[] bytes = new byte[20];
random.nextBytes(bytes);
base32 = new org.apache.commons.codec.binary.Base32();
secretKey = base32.encodeToString(bytes);
out.println(userIdentifiers[i] + "=" + secretKey);
}

8. Click 'Execute' and copy the output into a text editor. Sample output as follows:

mw@liferay.com=C2MEXYHY62VPCNKXKAEXL5NE5MMNUEHC
wm@liferay.com=VIBLX4DXHY474BNG5YOJAHZ3KNHIKW2H

9. Go to Control Panel > Configuration > System Settings > Security > TOTP 2FA

10. Enable the 'Login TOTP 2FA Enabled' checkbox

11. For each output line from step 8, populate a 'Temporary Secret Key Mappings' field, using the + icon to add additional mappings. Each onscreen field should contain a single mapping.

12. Click 'Save' to save and apply the changes

13. Logout of Liferay

14. Go to the Home page and click 'Login'. Confirm that the Authenticator Code field appears below the Password field

15. Download the Google Authenticator app to your phone and launch it

16. For each user, click the + icon and select 'Manual Entry'. For Account enter the identifier and for Key paste the secretKey then Save

17. Identify which user you want to login with, note the Authenticator Code for that user from the phone app and login with the user credentials and the Authenticator Code

**************************************
Notes
**************************************

i. Supported Liferay versions: DXP 7.1 however you can make some minor changes to run in DXP 7.0 (see DXP 7.0 support section below)
ii. 2 Factor Authentication Login must be explicitly configured and enabled after initial deployment, see 'Deployment & Setup Steps' below
iii. Securely generating, storing / retrieving and distributing the user specific secreyKey is not in scope for this project:
- The user specific secretKey should be securely generated and securely distributed to the end user
- The user specific secretKey is currently stored in plain text in System Settings for convenient retrieval, they should be securely stored and retrieved e.g. with a custom service builder service
iv. Ensure that the phone and server time are roughly the same, if not then the generated codes may not match when the comparison is done, as the code is only valid for 30 seconds
v. If the Google Authenticator code is red it means it is about to expire
- Wait until a new one is generated before trying as a time difference of a few seconds between the phone and server means it may not work
vi. Users with the Liferay Administrator role will always bypass TOTP 2FA on login and can leave the Authenticator Code field empty
vii. A bug in the Liferay DXP 7.1 Senna / Single Page Application (SPA) implementation unexpectedly prevents the Login Modal Dialog form body fields being included in the parameterMap passed through to the Authenticator 
- This prevents the Authenticator functioning as expected, as the Authenticator Code field is missing
- Senna is turned off for the Login form / portlet through the login.jsp fragment
iix. The project uses an auth.pipeline.post Authenticator. If the regular credentials are not valid then the custom Authenticator will not be triggered
ix. If you change company.security.auth.type (either through portal-ext.properties or the GUI then you will need to update the System Settings > Security > TOTP 2FA > Temporary Secret Key Mappings to use the new identifier for each user

**************************************
OSGi Bundles
**************************************

The project consists of the following OSGi bundles:

com.mw.totp-2fa.login.fragment
- Contains a fragment to overide Login Portlet login.jsp
- The fragment disables Senna for the login portlet (by adding data-senna-off="true" to the aui:form tag)
- The fragment adds a new dynamic include with key="com.liferay.login.web#/login.jsp#loginFieldsPost" below the Password field

com.mw.totp-2fa.login.auth
- Contains the Dynamic Include that adds the Authenticator Code field to the Login screen
- Contains an auth.pipeline.post Authenticator that extracts the Authenticator Code from the Login form and verifies it matches one generated based on the users secretKey and the current time

com.mw.totp-2fa.service
- Contains the System Settings > Security > TOTO 2FA Configuration
- Contains the TOTP Generator Interface and Implementations

**************************************
2 Factor Authentication App setup
**************************************

There are lots of 2 Factor Authentication Apps available in the Apple and Google App Stores. 

If you choose an alternative to Google Authenticator app, please note the following when adding a profile:

i. If prompted for type, select 'Time-based' rather than 'Counter-based'
ii. If prompted for interval, select 30 seconds
iii. If prompted for length, select 6 digits
iv. If prompted for algorithm, select SHA-1

**************************************
TOTP Implementation
**************************************

Time-based One-Time Password algorithm (TOTP) is an algorithm that computes a one-time password using a user specific shared secret key and the current time.

TOTP is widely used in two-factor authentication systems. For more info see:

- https://en.wikipedia.org/wiki/Time-based_One-time_Password_algorithm
- https://tools.ietf.org/html/rfc6238

This project uses the following TOTP implementation(s):

- https://github.com/jchambers/java-otp/ available here: https://search.maven.org/search?q=a:java-otp
- https://github.com/j256/two-factor-auth available here: https://mvnrepository.com/artifact/com.j256.two-factor-auth/two-factor-auth

The default implementation is java-otp, this can be switched through System Settings > Security > TOTP 2FA > TOTP 2FA Implementation.

**************************************
DXP 7.0 support
**************************************

You can use this project in DXP 7.0, it will function the same as DXP 7.1 with one minor difference, the Configuration appears under category 'TOTP 2FA' in System Settings.

To use in 7.0, make the following changes, do a Gradle > Refresh Gradle Project then Gradle clean and Gradle build to build the bundles.

totp-2fa-login-auth\build.gradle, com.liferay.portal.kernel, change version to "2.63.0"

totp-2fa-login-fragment\bnd.bnd, change the Fragment-Host bundle-version to be "2.0.7"
- to match the DXP 7.0 version


totp-2fa-service\build.gradle, com.liferay.portal.kernel, change version to "2.63.0"

totp-2fa-service\build.gradle, comment out: compileOnly group: "com.liferay", name: "com.liferay.configuration.admin.api", version: "1.0.1"
- the OSGi bundle doesn't exist in DXP 7.0

totp-2fa-service\src\main\java\com\mw\totp_2fa\config\TOTP_2FAConfigurationCategory.java, delete this class
- the interface doesn't exist in DXP 7.0
