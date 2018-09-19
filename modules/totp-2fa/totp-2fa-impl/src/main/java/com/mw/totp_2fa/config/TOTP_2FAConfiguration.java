package com.mw.totp_2fa.config;

import com.liferay.portal.configuration.metatype.annotations.ExtendedObjectClassDefinition;
import com.mw.totp_2fa.api.TOTP_2FAGenerator;

import aQute.bnd.annotation.metatype.Meta;
import aQute.bnd.annotation.metatype.Meta.Type;

@ExtendedObjectClassDefinition(category = "totp_2fa", scope = ExtendedObjectClassDefinition.Scope.SYSTEM)
@Meta.OCD(id = TOTP_2FAConfiguration.PID, localization = "content/ConfigurationLanguage", name = "configuration.name.totp_2fa")
public interface TOTP_2FAConfiguration {

	public static final String PID = "com.mw.totp_2fa.config.TOTP_2FAConfiguration";

	@Meta.AD(deflt = "false", required = false, type = Type.Boolean, name = "configuration.loginTotp2faEnabled.name", description = "configuration.loginTotp2faEnabled.desc")
	public boolean loginTotp2faEnabled();

	@Meta.AD(deflt = "", required = false, type = Type.String, name = "configuration.secretKeyMappings.name", description = "configuration.secretKeyMappings.desc")
	public String[] secretKeyMappings();
	
	@Meta.AD(deflt = "", required = false, type = Type.String, name = "configuration.loginTotp2faSkipUserRole.name", description = "configuration.loginTotp2faSkipUserRole.desc")
	public String loginTotp2faSkipUserRole();

	@Meta.AD(deflt = "6", required = false, optionLabels = {"6", "7", "8"}, optionValues = {"6", "7", "8"}, type = Type.Integer, name = "configuration.authenticatorCodeLength.name", description = "configuration.authenticatorCodeLength.desc")
	public int authenticatorCodeLength();	
}