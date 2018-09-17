package com.mw.totp_2fa.login.auth;

import com.liferay.portal.configuration.metatype.bnd.util.ConfigurableUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.CompanyConstants;
import com.liferay.portal.kernel.model.Role;
import com.liferay.portal.kernel.model.RoleConstants;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.security.auth.AuthException;
import com.liferay.portal.kernel.security.auth.Authenticator;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.util.Validator;
import com.mw.totp_2fa.api.TOTP_2FAGenerator;
import com.mw.totp_2fa.config.TOTP_2FAConfiguration;
import com.mw.totp_2fa.login.auth.constants.LoginConstants;
import com.mw.totp_2fa.util.TOTP_2FAUtil;

import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

@Component(
	    immediate = true, property = {"key=auth.pipeline.post"},
	    service = Authenticator.class,
	    configurationPid = TOTP_2FAConfiguration.PID
)
public class TOTP_2FAPostAuthenticator implements Authenticator {

	@Override
	public int authenticateByScreenName(long companyId, String screenName, String password,
			Map<String, String[]> headerMap, Map<String, String[]> parameterMap) throws AuthException {
		
		if (!configuration.loginTotp2faEnabled()) return Authenticator.SUCCESS;

		return verifyAuthenticatorCode(CompanyConstants.AUTH_TYPE_SN, companyId, screenName, parameterMap);
	}

	@Override
	public int authenticateByEmailAddress(long companyId, String emailAddress, String password,
			Map<String, String[]> headerMap, Map<String, String[]> parameterMap) throws AuthException {
		
		if (!configuration.loginTotp2faEnabled()) return Authenticator.SUCCESS;

		return verifyAuthenticatorCode(CompanyConstants.AUTH_TYPE_EA, companyId, emailAddress, parameterMap);
	}

	@Override
	public int authenticateByUserId(long companyId, long userId, String password, Map<String, String[]> headerMap,
			Map<String, String[]> parameterMap) throws AuthException {
		
		if (!configuration.loginTotp2faEnabled()) return Authenticator.SUCCESS;
		
		return verifyAuthenticatorCode(CompanyConstants.AUTH_TYPE_ID, companyId, userId, parameterMap);
	}

	private int verifyAuthenticatorCode(String authType, long companyId, Object identifier, Map<String, String[]> parameterMap)
			throws AuthException {
		
		User user = fetchUser(authType, companyId, identifier);
		
		if (user == null) {
			if (_log.isInfoEnabled()) {
				_log.info("TOTP_2FAPostAuthenticator, return failure as user null for: " + identifier);	
			}
			
			return Authenticator.FAILURE;
		} else if (isAdministrator(user)) {
			if (_log.isInfoEnabled()) {
				_log.info("TOTP_2FAPostAuthenticator, return success as user is Administrator (skipping TOTP 2FA check) for: " + identifier);	
			}			
			
			return Authenticator.SUCCESS;
		}
		
		String authenticatorCode = null;
		
		if (parameterMap.containsKey(LoginConstants.AUTHENTICATOR_CODE_FIELD) && parameterMap.get(LoginConstants.AUTHENTICATOR_CODE_FIELD) != null) {
			authenticatorCode = parameterMap.get(LoginConstants.AUTHENTICATOR_CODE_FIELD)[0];
			
			if (Validator.isNull(authenticatorCode)) {
				if (_log.isInfoEnabled()) {
					_log.info("TOTP_2FAPostAuthenticator, return failure as authenticatorCode null for: " + identifier);	
				}
				
				return Authenticator.FAILURE;
			}
			
			// Remove any whitespace within the code e.g. if user used xxx xxx syntax to match Google Authenticator.
			authenticatorCode = authenticatorCode.replace(" ", "");

			if (authenticatorCode.trim().length() != configuration.authenticatorCodeLength()) {
				if (_log.isInfoEnabled()) {
					_log.info("TOTP_2FAPostAuthenticator, return failure as authenticatorCode not " + configuration.authenticatorCodeLength() + " chars for: " + identifier);
				}
				
				return Authenticator.FAILURE;
			}
		} else {
			if (_log.isInfoEnabled()) {
				_log.info("TOTP_2FAPostAuthenticator, return failure as authenticatorCode missing for: " + identifier);	
			}
			
			return Authenticator.FAILURE;
		}

		String secretKey = TOTP_2FAUtil.getSecretKeyByIdentifier((String)identifier, configuration.secretKeyMappings());
			
		if (Validator.isNull(secretKey)) {
			if (_log.isInfoEnabled()) {
				_log.info("TOTP_2FAPostAuthenticator, return failure as stored secretKey missing for: " + identifier);	
			}
			
			return Authenticator.FAILURE;				
		}
			
		String generatedAuthenticatorCode = totpGenerator.getTOTPCode(configuration.totp2faImplementation(), secretKey, configuration.authenticatorCodeLength());		
			
		if (Validator.isNull(generatedAuthenticatorCode)) {
			if (_log.isInfoEnabled()) {
				_log.info("TOTP_2FAPostAuthenticator, return failure as generatedAuthenticatorCode null for: " + identifier);	
			}
			
			return Authenticator.FAILURE;				
		}
			
		if (_log.isDebugEnabled()) {
			_log.debug("TOTP_2FAPostAuthenticator, authenticatorCode: " + authenticatorCode + ", generatedAuthenticatorCode: " + generatedAuthenticatorCode + " for: " + identifier);
		}
		
		if (authenticatorCode.equalsIgnoreCase(generatedAuthenticatorCode)) {
			if (_log.isDebugEnabled()) {
				_log.debug("TOTP_2FAPostAuthenticator, return success as authenticatorCode matches generatedAuthenticatorCode for: " + identifier);
			}

			return Authenticator.SUCCESS;
		} else {
			if (_log.isInfoEnabled()) {
				_log.info("TOTP_2FAPostAuthenticator, return failure as authenticatorCode mismatch for: " + identifier);
			}			
		}

		return Authenticator.FAILURE;
	}

	private User fetchUser(String authType, long companyId, Object identifier) {
		User user = null;

		if (authType.equalsIgnoreCase(CompanyConstants.AUTH_TYPE_EA)) {
			user = userLocalService.fetchUserByEmailAddress(companyId, (String)identifier);	
		} else if (authType.equalsIgnoreCase(CompanyConstants.AUTH_TYPE_SN)) {
			user = userLocalService.fetchUserByScreenName(companyId, (String)identifier);
		} else if (authType.equalsIgnoreCase(CompanyConstants.AUTH_TYPE_ID)) {
			user = userLocalService.fetchUserById((long)identifier);
		}
		
		return user;
	}
	
	private boolean isAdministrator(User user) {
		
		for (Role role : user.getRoles()) {
			if (role.getName().equalsIgnoreCase(RoleConstants.ADMINISTRATOR)) {
				return true;
			}
		}
		
		return false;
	}
	
	@Activate
	@Modified
	protected void activate(Map<String, Object> properties) {
		configuration = ConfigurableUtil.createConfigurable(TOTP_2FAConfiguration.class, properties);

		if (_log.isInfoEnabled()) {
			_log.info("*********************************************");
			_log.info("configuration.loginTOTPEnabled: " + configuration.loginTotp2faEnabled());
			_log.info("configuration.authenticatorCodeLength: " + configuration.authenticatorCodeLength());
			_log.info("configuration.totp2faImplementation: " + configuration.totp2faImplementation());
			_log.info("*********************************************");
		}
	}

	private volatile TOTP_2FAConfiguration configuration;	

	@Reference(cardinality=ReferenceCardinality.MANDATORY)
	private UserLocalService userLocalService;
	
	@Reference(cardinality=ReferenceCardinality.MANDATORY)
	private volatile TOTP_2FAGenerator totpGenerator;

	private static Log _log = LogFactoryUtil.getLog(TOTP_2FAPostAuthenticator.class);	
}