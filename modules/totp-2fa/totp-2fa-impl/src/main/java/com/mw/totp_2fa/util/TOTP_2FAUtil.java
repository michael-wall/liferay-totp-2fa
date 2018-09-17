package com.mw.totp_2fa.util;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.Validator;

public class TOTP_2FAUtil {
	
	public static String getSecretKeyByIdentifier(String identifier, String mappingsString) {
		if (Validator.isNull(identifier) || Validator.isNull(mappingsString)) return null;
		
		String[] mappingsArray = mappingsString.split(",");

		for (String mappingString : mappingsArray) {
			String[] mappingArray = mappingString.split("\\=");
			
			if (Validator.isNotNull(mappingArray[0])) {
				if (mappingArray[0].trim().equalsIgnoreCase(identifier.trim())) {
					if (Validator.isNotNull(mappingArray[1])) {
						return mappingArray[1].trim();
					} else {
						return null;
					}
				}
			}
		}

		return null;
	}		
	
	private static Log _log = LogFactoryUtil.getLog(TOTP_2FAUtil.class);		
}