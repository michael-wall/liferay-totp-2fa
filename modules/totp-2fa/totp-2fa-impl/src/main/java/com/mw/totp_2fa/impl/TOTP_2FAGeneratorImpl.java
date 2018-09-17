package com.mw.totp_2fa.impl;

import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;
import com.j256.twofactorauth.TimeBasedOneTimePasswordUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.Validator;
import com.mw.totp_2fa.api.TOTP_2FAGenerator;

import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base32;
import org.osgi.service.component.annotations.Component;

@Component(immediate = true, service = TOTP_2FAGenerator.class)
public class TOTP_2FAGeneratorImpl implements TOTP_2FAGenerator {
	
	@Override
	public String getTOTPCode(String implementation, String secretKey, int authenticatorCodeLength) {
		
		if (Validator.isNull(implementation) || implementation.trim().equalsIgnoreCase(TOTP_2FAGenerator.TOTP_API_IMPLEMENTATIONS.JAVA_OPT.trim())) {
			return getTOTPCodeJavaOpt(secretKey, authenticatorCodeLength);
		} else if (implementation.trim().equalsIgnoreCase(TOTP_2FAGenerator.TOTP_API_IMPLEMENTATIONS.J256.trim())) {
			return getTOTPCodeJ256(secretKey);
		} else {
			return getTOTPCodeJavaOpt(secretKey, authenticatorCodeLength);
		}
	}

	//https://github.com/jchambers/java-otp/
	//https://search.maven.org/search?q=a:java-otp
	private String getTOTPCodeJavaOpt(String secretKey, int authenticatorCodeLength) {
		
		TimeBasedOneTimePasswordGenerator totpGenerator = null;

		try {
			totpGenerator = new TimeBasedOneTimePasswordGenerator(authenticatorCodeLifeDuration, TimeUnit.SECONDS, authenticatorCodeLength, TimeBasedOneTimePasswordGenerator.TOTP_ALGORITHM_HMAC_SHA1);

			Base32 base32 = new Base32();
			byte[] optBytes = base32.decode(secretKey);
			
			SecretKeySpec optSecretKey = new SecretKeySpec(optBytes, TimeBasedOneTimePasswordGenerator.TOTP_ALGORITHM_HMAC_SHA1);
			
			String toptCode = String.valueOf(totpGenerator.generateOneTimePassword(optSecretKey, new Date()));
			
			if (_log.isDebugEnabled()) {
				_log.debug("TOTP_2FAGeneratorImpl.getTOTPCodeJavaOpt: " + toptCode);
			}
			
			return toptCode;
			
		} catch (NoSuchAlgorithmException e) {
			_log.error("NoSuchAlgorithmException generating TOTP with secretKey and authenticatorCodeLength: " + authenticatorCodeLength + ", " + e.getClass().getCanonicalName() + ", " + e.getMessage());
			_log.error(e);
			
			return null;
		} catch (InvalidKeyException e) {
			_log.error("InvalidKeyException generating TOTP with secretKey and authenticatorCodeLength: " + authenticatorCodeLength + ", " + e.getClass().getCanonicalName() + ", " + e.getMessage());
			_log.error(e);
			
			return null;
		}
	}	

	//https://github.com/j256/two-factor-auth
	//https://mvnrepository.com/artifact/com.j256.two-factor-auth/two-factor-auth
	private String getTOTPCodeJ256(String secretKey) {

        try {
			String totpCode = TimeBasedOneTimePasswordUtil.generateCurrentNumberString(secretKey);
			
			if (_log.isDebugEnabled()) {
				_log.info("TOTP_2FAGeneratorImpl.getTOTPCodeJ256: " + totpCode);
			}
			
			return totpCode;
		} catch (GeneralSecurityException e) {
			_log.error("GeneralSecurityException generating TOTP with secretKey, " + e.getClass().getCanonicalName() + ", " + e.getMessage());

			_log.error(e);
			
			return null;
		}
	}
	
	private static Log _log = LogFactoryUtil.getLog(TOTP_2FAGeneratorImpl.class);	
}
