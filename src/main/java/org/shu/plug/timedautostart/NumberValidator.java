package org.shu.plug.timedautostart;

import com.biglybt.pif.ui.UIInputValidator;

public class NumberValidator implements UIInputValidator{
	
	public String validate(String input) {
		try{
			if (Double.valueOf(input).isNaN()){
				return "shu.plugin.validator.notnumber";
			}
		} catch(NumberFormatException ex){
			return "shu.plugin.validator.notnumber";
		}
		return null;
	}
}
