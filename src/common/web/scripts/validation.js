//
// Validation functions
// Gavin Cornwell 30-11-2005
//

/**
 * Informs the user of the given 'message', if 'showMessage' is true.
 * If 'showMessage' is true focus is given to the 'control'.
 */
function informUser(control, message, showMessage)
{
	if (showMessage)
   {
      alert(message);
      control.focus();
   }
}

/**
 * Try to validate value set by picker. If value of picker is not set, then check if value has been set manually.
 * @return true if value has been set
 * @author Ats Uiboupin
 */
function validateSearchMandatory(control, message, showMessage) {
   if (control.value == null || control.value.length == 0) {
      var hiddenIn = $jQ(control);
      var inputs = hiddenIn.prev().find("tbody tr td input");
      if(null != inputs.get(0)) {
         var manualValue = inputs.val().trim();
         if (manualValue != null && manualValue.length != 0) {
            return true; // value set manually
         }
      }
   } else {
      return true; // value set using picker
   }
   informUser(control, message, showMessage);
   return false;
}
/**
 * Ensures the value of the 'control' is not null or 0.
 *
 * @return true if the mandatory validation passed
 */
function validateMandatory(control, message, showMessage)
{
   var result = true;
   
   if (control != null && (control.value == null || jQuery.trim(control.value).length == 0))
   {
      informUser(control, message, showMessage);
      result = false;
   }
   
   return result;
}

/**
 * Ensures the value of the 'control' is more than 'min' and less than 'max'.
 *
 * @return true if the number range validation passed
 */	
function validateNumberRange(control, min, max, message, showMessage)
{
   var result = true;
   
   if (isNaN(control.value) || control.value < min || control.value > max)
   {
      informUser(control, message, showMessage);
      result = false;
   }
   
   return result;
}

/**
 * Ensures the value of the 'control' is a long number.<br>
 * No point of validating range, as in js max and min values seem to be smaller than in Java: [-9223372036854776000;9223372036854776000]<br>
 *  // isNumber = (isNumber && input <= longMaxValue && input >= longMinValue);<br>
 * @return true if the value is a number
 */
function validateIsLongNumber(control, message, showMessage)
{
   var input = control.value;
   var validChars = "0123456789";
   var isNumber=true;
   var char;
   var longMaxValue =  9223372036854775807;
   var longMinValue = -9223372036854775808;
   for (var i = 0; i < input.length && isNumber == true; i++) { 
      char = input.charAt(i); 
      if (validChars.indexOf(char) == -1) {
         if(!(i == 0 && char =="-")) {
            isNumber = false;
         }
      }
   }
   //   no point of validating range, as in js max and min values seem to be smaller than in Java: [-9223372036854776000;9223372036854776000]
   //   var isNumber = (isNumber && input <= longMaxValue && input >= longMinValue);
   if(!isNumber) {
      informUser(control, message, showMessage);
   }
   return isNumber;
}


/**
 * Ensures the value of the 'control' is a number.
 *
 * @return true if the value is a number
 */
function validateIsNumber(control, message, showMessage)
{
   var result = true;
   
   if (isNaN(control.value))
   {
      informUser(control, message, showMessage);
      result = false;
   }
   
   return result;
}

/**
 * Ensures the value of the 'control' has a string length more than 'min' and less than 'max'.
 *
 * @return true if the string length validation passed
 */
function validateStringLength(control, min, max, message, showMessage)
{
   var result = true;

   var controlValLength = jQuery.trim(control.value).length;
   if (controlValLength < min || controlValLength > max)
   {
      informUser(control, message, showMessage);
      result = false;
   }
   
   return result;
}

/**
 * Ensures the value of the 'control' matches the 'expression' if 'requiresMatch' is true. 
 * Ensures the value of the 'control' does not match the 'expression' if 'requiresMatch' is false.
 * 
 * @return true if the regex validation passed
 */
function validateRegex(control, expression, requiresMatch, matchMessage, noMatchMessage, showMessage)
{
   var result = true;
   
   var pattern = new RegExp(decode(expression));
   var matches = pattern.test(control.value);
   
   if (matches != requiresMatch)
   {
      if (requiresMatch)
      {
         informUser(control, noMatchMessage, showMessage);
      }
      else
      {
         informUser(control, matchMessage, showMessage);
      }
      
      result = false;
   }
   
   return result;
}

/**
 * Ensures the value of the 'control' does not contain any illegal characters.
 * 
 * @return true if the file name is valid
 */
function validateName(control, message, showMessage)
{
   var result = true;
   var pattern = /([\"\*\\\>\<\?\/\:\|]+)|([ ]+$)|([\.]?[\.]+$)/;
   var trimed = control.value.replace(/^\s\s*/, '').replace(/\s\s*$/, '');
   var idx = trimed.search(pattern);
   if (idx != -1)
   {
      informUser(control, "'" + trimed.charAt(idx) + "' " + message, showMessage);
      result = false;
   }
   
   return result;
}

/**
 * Ensures the value of the 'control' is a valid date.
 * 
 * @return true if the date is valid
 */
function validateDate(control, message, showMessage)
{
   // PropertySheet must handle mandatory fields left blank
   if(control.value.length < 1)
   {
	   return true;
   }
   
   var result = true;
    
   // http://www.regexlib.com/REDetails.aspx?regexp_id=762
   var pattern = 
/^(?=\d)(?:(?!(?:(?:0?[5-9]|1[0-4])(?:\.|-|\/)10(?:\.|-|\/)(?:1582))|(?:(?:0?[3-9]|1[0-3])(?:\.|-|\/)0?9(?:\.|-|\/)(?:1752)))(31(?!(?:\.|-|\/)(?:0?[2469]|11))|30(?!(?:\.|-|\/)0?2)|(?:29(?:(?!(?:\.|-|\/)0?2(?:\.|-|\/))|(?=\D0?2\D(?:(?!000[04]|(?:(?:1[^0-6]|[2468][^048]|[3579][^26])00))(?:(?:(?:\d\d)(?:[02468][048]|[13579][26])(?!\x20BC))|(?:00(?:42|3[0369]|2[147]|1[258]|09)\x20BC))))))|2[0-8]|1\d|0?[1-9])([-.\/])(1[012]|(?:0?[1-9]))\2((?=(?:00(?:4[0-5]|[0-3]?\d)\x20BC)|(?:\d{4}(?:$|(?=\x20\d)\x20)))\d{4}(?:\x20BC)?)(?:$|(?=\x20\d)\x20))?$/;
   var matches = pattern.test(control.value);
   
   if (!matches)
   {
      informUser(control, message, showMessage);
      result = false;
   }
   
   return result;
}

/**
 * Decodes the given string
 * 
 * @param str The string to decode
 * @return The decoded string
 */
function decode(str)
{
    var s0, i, j, s, ss, u, n, f;
    
    s0 = "";                // decoded str

    for (i = 0; i < str.length; i++)
    {   
        // scan the source str
        s = str.charAt(i);

        if (s == "+") 
        {
            // "+" should be changed to SP
            s0 += " ";
        }       
        else 
        {
            if (s != "%") 
            {
                // add an unescaped char
                s0 += s;
            }     
            else
            {               
                // escape sequence decoding
                u = 0;          // unicode of the character

                f = 1;          // escape flag, zero means end of this sequence

                while (true) 
                {
                    ss = "";        // local str to parse as int
                    for (j = 0; j < 2; j++ ) 
                    {  
                        // get two maximum hex characters for parse
                        sss = str.charAt(++i);

                        if (((sss >= "0") && (sss <= "9")) || ((sss >= "a") && (sss <= "f"))  || ((sss >= "A") && (sss <= "F"))) 
                        {
                            ss += sss;      // if hex, add the hex character
                        } 
                        else 
                        {
                            // not a hex char., exit the loop
                            --i; 
                            break;
                        }    
                    }

                    // parse the hex str as byte
                    n = parseInt(ss, 16);

                    // single byte format
                    if (n <= 0x7f) { u = n; f = 1; }

                    // double byte format
                    if ((n >= 0xc0) && (n <= 0xdf)) { u = n & 0x1f; f = 2; }

                    // triple byte format
                    if ((n >= 0xe0) && (n <= 0xef)) { u = n & 0x0f; f = 3; }

                    // quaternary byte format (extended)
                    if ((n >= 0xf0) && (n <= 0xf7)) { u = n & 0x07; f = 4; }

                    // not a first, shift and add 6 lower bits
                    if ((n >= 0x80) && (n <= 0xbf)) { u = (u << 6) + (n & 0x3f); --f; }

                    // end of the utf byte sequence
                    if (f <= 1) { break; }         

                    if (str.charAt(i + 1) == "%") 
                    { 
                        // test for the next shift byte
                        i++ ; 
                    }                   
                    else 
                    {
                        // abnormal, format error
                        break;
                    }                   
                }

                // add the escaped character
                s0 += String.fromCharCode(u);

            }
        }
    }

    return s0;

}