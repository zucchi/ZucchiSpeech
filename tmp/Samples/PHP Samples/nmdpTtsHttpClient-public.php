<?php
/*
 * nmdpTtsHttpClient-public.php
 *
 * This is a sample PHP script that shows how to use the NMDP HTTP Client Interface for
 *	Text-to-Speech (TTS) requests using the GET method
 * 
 * NOTE: You must install and configure your PHP run-time environment with the http client extension (php_http.dll on windows, http.so on Mac/Linux).
 *		 Use the following command to download and build the client on MAC/Linux (assumes 32-bit processor): 
 *				
 *			sudo CFLAGS="-arch i386" pecl install pecl_http
 *
 * @copyright  Copyright (c) 2010 Nuance Communications, inc. (http://www.nuance.com)
 *
 * @Created	: April 28, 2011
 * @Author	: Peter Freshman
 */

/*
 **********************************************************************************************************
 * Client Interface Parameters:
 *
 * appId: 		You received this by email when you registered
 * appKey:	 	You received this as a 64-byte Hex array when you registered.
 * 				If you provide us with your username, we can convert this to a 128-byte string for you.
 * id: 			Device Id is any character string. Typically a mobile device Id, but for test purposes, use the default value
 * voice:		Provide either a voice or a language to be used to generate the audio. If you provide both a voice and language, the
 * ttsLang:		language value will be ignored. Check out our NMDP FAQ for the current list of supported languages and voices.
 *				NOTE: Included in this sample is reference to a file that includes the latest set of available voices and languages.
 * codec:		The desired audio format.
 * text:		The text to be passed to the client interface and converted to audio.
 *
 *********************************************************************************************************
 *
 * Additional parameters to help drive this sample script
 *
 * Selection:	This is a radio button on the HTML form to allow you to select voice or language as the parameter to pass
 * outputDir:	Where to write the audio to file.
 *
 *********************************************************************************************************
 */
$defaultAppId = "Insert Your App Id";
$defaultAppKey = "Insert Your 128-Byte String App Key";
$defaultDeviceId = "0000";
$defaultVoice = "Samantha";
$defaultLanguage = "en_US";
$defaultSelection = "voice";
$defaultCodec = "wav";
$defaultText = "This is a test.";

$outputDir = "./output/";

require_once "./data/voices.php";	// Supported Voices and Languages available with this interface
require_once "./data/codecs.php";	// Supported Codecs for the returned audio

/*
 * If this script has been POSTED to, use the submitted values.
 * Otherwise, use the default values set above.
 */
$appId = isset($_POST['appId']) ? trim($_POST['appId']) : $defaultAppId;
$appKey = isset($_POST['appKey']) ? trim($_POST['appKey']) : $defaultAppKey;
$deviceId = isset($_POST['deviceId']) ? trim($_POST['deviceId']) : $defaultDeviceId;
$voice = isset($_POST['voice']) ? trim($_POST['voice']) : $defaultVoice;
$language = isset($_POST['language']) ? trim($_POST['language']) : $defaultLanguage;
$selection = isset($_POST['sel']) ? trim($_POST['sel']) : $defaultSelection;
$codec = isset($_POST['codec']) ? trim($_POST['codec']) : $defaultCodec;
$text = isset($_POST['text']) ? trim($_POST['text']) : $defaultText;

/*
 * Create some global dropdown lists that will be used on the HTML page
 */
$voices_dropdown = "";
$languages_dropdown = "";
$codecs_dropdown = "";

/*
 * This function is a utility to help create the dropdown lists for the HTML page
 * We need 3 lists:
 *	1. The list of voices available to choose from. Samantha (US English) has been set as the default above)
 *	2. The list of languages available to choose from. These must be provided in xx_XX format.
 *	3. The list of supported codecs. Specify the format of the audio to be returned. .wav 16Khz is the default.
 */
function createDropDowns()
{
	global $voices_dropdown;
	global $languages_dropdown;
	global $codecs_dropdown;
	global $voices;
	global $voice;
	global $language;
	global $codec;


	// Initialize the voice list with 'Test All Voices'. We use the global $voice parameter to determine if
	//	this option is already selected.
	$selected = ($voice == 'Test All Voices') ? "selected" : "";
	$voices_dropdown = '<option '. $selected .' value="Test All Voices">Test All Voices</option>';

	// Initialize the language list with 'Test All Languages'. We use the global $language parameter to determine if
	//	this option is already selected.
	$selected = ($language == 'Test All Languages') ? "selected" : "";
	$languages_dropdown = '<option '. $selected .' value="Test All Languages">Test All Languages</option>';

	// For each dropdown list loop through the available sets of voices and languages and set the options.
	//	And make sure the one selected by the user (if this is a POST) is pre-selected again on re-display of the page.
	foreach($GLOBALS['voices'] as $v)
	{
		/*
		 * v[0]: Language Name
		 * v[1]: Language Code (this is the value that actually gets passed to the interface)
		 * v[2]: Voice (a language may have multiple voices available...)
		 */

		// Display voices as "Language Name - Voice" (ie: "US English - Samantha")
		$selected = ($voice == $v[2]) ? "selected" : "";
		$voices_dropdown .= '<option '. $selected .' value="'. $v[2] .'">'. $v[0] .'-'. $v[2] .'</option>';

		// Display languages as "Language Name - Language Code" (ie: "US English - en_US")
		$selected = ($language == $v[1]) ? "selected" : "";
		$languages_dropdown .= '<option '. $selected .' value="'. $v[1] .'">'. $v[0] .'-'. $v[1] .'</option>';
	}

	// Do the same for the codecs...
	foreach($GLOBALS['codecs'] as $c)
	{
		/*
		 * c[0]: Codec value to use for a GET request
		 * c[1]: Codec value to use for a POST request
		 * c[2]: File extension to use
		 */

		// Display codecs as "GET value - File extension" (ie: "wav - wav")
		$selected = ($codec == $c[0]) ? "selected" : "";
		$codecs_dropdown .= '<option '. $selected .' value="'. $c[0] .'">'. $c[0] .'-'. $c[2] .'</option>';
	}
}

/*
 * This little routine finds the language associated with a given voice. This helps when
 *	we're building the audio filename (language code-voice.file extension) because if voice is selected as
 * 	the method of generating the audio, the selected language in the dropdown list is most likely not
 *	in sync with the selected voice.
 */
function findLanguageCode( $voice )
{
	global $voices;
	global $defaultLanguage;

	foreach($voices as $v)
	{
		//$v[1] = language code
		//$v[2] = voice
		if( $voice == $v[2] )
			return $v[1];
	}

	return $defaultLanguage;
}

/*
 * This function will loop through every voice available and create an audio sample for each one.
 *	Use this to verify availability of voices.
 *
 */
function testAll($selection, $appId, $appKey, $id, $codec, $text)
{
	global $voices;
	$testResults = array();

	foreach($voices as $v)
	{
		// We need to reset the script timer for each TTS request, otherwise the script engine will time out...
		set_time_limit(25);

		/*
		 * v[0]: Language Name
		 * v[1]: Language Code (this is the value that actually gets passed to the interface)
		 * v[2]: Voice (a language may have multiple voices available...)
		 */

		// This function builds the URI with GET parameters
		$url = buildRequestString($selection, $appId, $appKey, $id, $v[2], $v[1], $codec, $text);

		// This function will execute the HTTP Request and pass back the response
		$m = executeTtsRequest($url);

		// Save each response to a results array that can be processed later for display
		$testResults[] = processTestResults($m, $v[1], $v[2]);
	}

	// Loop through the result set and display them on the page
	displayTestResults($testResults);
}

/*
 * This function simply helps build the HTTP request with the required parameters.
 *
 * Note the URI to be used in your application
 * Note the parameter values that must be passed in for a GET request
 * Note that we pass in either 'voice' or 'ttsLang'. If you pass in both, 'voice' wins
 *
 */
function buildRequestString($selection, $appId, $appKey, $id, $voice, $language, $codec, $text)
{
	// set the base URL
	$url = 'https://tts.nuancemobility.net:443/NMDPTTSCmdServlet/tts';

	// set the name/value pairs to be passed in as part of the GET request
	if( $selection == 'voice' )
	{
		$fields = array(
						'appId'=>urlencode($appId),
						'appKey'=>urlencode($appKey),
						'id'=>urlencode($id),
						'voice'=>urlencode($voice),		// voice is optional and will override ttsLang if present
						'codec'=>urlencode($codec),
						'text'=>urlencode($text)
					);
	}
	else
	{
		$fields = array(
						'appId'=>urlencode($appId),
						'appKey'=>urlencode($appKey),
						'id'=>urlencode($id),
						'ttsLang'=>urlencode($language),
						'codec'=>urlencode($codec),
						'text'=>urlencode($text)
					);
	}

	// Build the name=value string
	$fields_string = "";
	foreach($fields as $key=>$value) { $fields_string .= $key.'='.$value.'&'; }
	$fields_string = rtrim($fields_string,'&');

	// Decorate the URL with the name/value pairs
	$url .= '?' . $fields_string;

	// And we're done.
	return $url;
}

/*
 * This function executes the HTTP GET Request for us
 *
 * The HTTP response will be returned to the calling routine
 *
 */
function executeTtsRequest($url)
{
	$r = new HttpRequest($url, HttpRequest::METH_GET);

	try {
		$m = $r->send();

		return $m;
	} catch (HttpException $ex) {
	    echo $ex;
	}
}

/*
 * This function processes the results of a single TTS request
 *
 * The goal here is to display the response details so you can get
 * a feel for what to expect when things are working correctly and
 * when they're not...
 *
 * And we write the audio to file in this routine
 *
 */
function processResults($m, $language, $voice)
{
	global $outputDir;
	global $codec;
	global $codecs;

	$fileExtension = ".wav";	//default

	// For the selected Codec, find the appropriate file extension
	foreach ($codecs as $c)
	{
		if ($codec == $c[0]) {
			$fileExtension = '.' . $c[2];
			break;
		}
	}

	// Grab the Response headers for display
	// In a real-world application, it's important to grab the nuance-generated session id for debug purposes
	$respHeaders = var_export($m->getHeaders(), true);

	// Grab Response Code and Status for display. Again, in a real-world app, you would use these to determine
	//	how to respond to the calling application or user. Check the technical documentation for error code
	//	and error status details.
	$respCode = $m->getResponseCode();
	$respStatus = $m->getResponseStatus();

	// We'll simply display the response details in an HTML table
	echo "<table border=1>
	<tr><td colspan=2>HTTP Response Details</td><td></td></tr>
	<tr><td>Response Headers</td><td>$respHeaders</td></tr>
	<tr><td>Response Code</td><td>$respCode</td></tr>
	<tr><td>Response Status</td><td>$respStatus</td></tr>
	</table><br>";

	// Now, let's create a file to write the audio to
	$wavFile = $outputDir . $language . "-" . $voice . $fileExtension;
	$fh = fopen($wavFile, 'w') or die("can't open file");
	fwrite($fh, $m->getBody());
	fclose($fh);

	// And provide a link to the audio file so you can listen to the results
    echo "<br><br>";
	echo '<a href="' . $wavFile . '" alt="' . $wavFile . '">Listen to TTS Recording</a>';
	echo "<br><br>";

	// Done.
}

/*
 * This function processes the results of an HTTP request when testing
 *	the entire list of voices. It's similar to the function above, but
 *	instead of displaying the results immediately to the HTML page, we
 *	store the results in a results array to be displayed after all voices
 *	have been tested.
 */
function processTestResults($m, $language, $voice)
{
	global $outputDir;
	global $codec;
	global $codecs;

	$fileExtension = ".wav";	//default
	foreach ($codecs as $c)
	{
		if ($codec == $c[0]) {
			$fileExtension = '.' . $c[2];
			break;
		}
	}

	//$respHeaders = var_export($m->getHeaders(), true);
	$respCode = $m->getResponseCode();
	$respStatus = $m->getResponseStatus();


	$wavFile = $outputDir . $language . "-" . $voice . $fileExtension;
	$fh = fopen($wavFile, 'w') or die("can't open file");
	fwrite($fh, $m->getBody());
	fclose($fh);

	$resultsArray = array($language, $voice, $respCode, $respStatus, $wavFile);

	return $resultsArray;
}

/*
 * This function simply loops through all the test results captured when 'Test All Voices'
 * 	is selected, creating an HTML table showing response details and a link to the resulting
 *	audio file.
 */
function displayTestResults($testResults)
{
	echo 	"<table border=1>
			<tr><td colspan=5>Test Results</td><td></td></tr>
			<tr><td>Language</td><td>Voice</td><td>HTTP Code</td><td>HTTP Status</td><td>Wav File</td></tr>";

	foreach( $testResults as $result )
	{
		echo "<tr><td>$result[0]</td>
				<td>$result[1]</td>
				<td>$result[2]</td>
				<td>$result[3]</td>";
		echo 	'<td><a href="' . $result[4] . '" alt="' . $result[4] . '">Listen to TTS Recording</a></td></tr>';
	}

	echo "</table><br>";
}

// Before displaying the HTML below, create the drowpdown lists...
createDropDowns();
?>

<!-- Display our Web Page with a Form to collect HTTP Client Parameters -->
<html>
<body>
<form name="httpTest" action="nmdpTtsHttpClient-public.php" method="post">
<center>
<div style="width: 90%;">
<fieldset><legend>Enter Parameters to Test the NMDP HTTP Servlet</legend>
<br>
<table>
<!-- BEGIN -->

<!-- appId -->
<tr><td>App ID:&nbsp;</td><td><input type="text" name="appId" size="80" value="<?php echo $appId;?>"></td></tr>

<!-- appKey -->
<tr><td>App Key:&nbsp;</td><td><textarea name="appKey" rows="5" cols="80"><?php echo $appKey; ?></textarea></td></tr>

<!-- id -->
<tr><td>Device ID:&nbsp;</td><td><input type="text" name="deviceId" value="<?php echo $deviceId; ?>"></td></tr>
<tr><td colspan="2"><font color="green"><br>Choose either a voice or a language to test. <br>Please note that selecting 'Voice' will override any Language selection.</font></td></tr>

<!-- voice -->
<tr><td>Voice:&nbsp;</td><td>
<select name="voice" style="width:220;"><?php echo $voices_dropdown; ?></select>&nbsp;
<input type="radio" name="sel" value="voice" <?php echo ($selection == 'voice') ? "checked" : ""; ?>></td></tr>

<!-- ttsLang -->
<tr><td>Language:&nbsp;</td><td>
<select name="language" style="width:220;"><?php echo $languages_dropdown ?></select>&nbsp;
<input type="radio" name="sel" value="language" <?php echo ($selection == 'language') ? "checked" : ""; ?>></td></tr>

<!-- codec -->
<tr><td>Codec:&nbsp;</td><td>
<select name="codec" style="width:220;"><?php echo $codecs_dropdown?></select></td></tr>
<tr><td><br></td></tr>

<!-- text -->
<tr><td>Text to Convert: </td><td><textarea name="text" rows="5" cols="80"><?php echo $text; ?></textarea></td></tr>
<tr><td colspan="2" align="center"><br><input type="submit" name="Submit" /></td></tr>

<!-- END -->
</table>
<br>
</fieldset>
<br>

<?php
if( isset($_POST['appKey']) ) {

	// Let's display what's been provided for Form data so we can see what will be passed
	//	to the HTTP client interface
	echo "<fieldset><legend>Response Data</legend>
			<table border=1 width=80%>
			<tr><td colspan=2>Submitted Data</td></tr>
			<tr><td>App ID</td><td>$appId</td></tr>
			<tr><td>App Key</td><td>$appKey</td></tr>
			<tr><td>Device ID</td><td>$deviceId</td></tr>
			<tr><td>Voice</td><td>$voice</td></tr>
			<tr><td>Language</td><td>$language</td></tr>
			<tr><td>Selection</td><td>$selection</td></tr>
			<tr><td>Codec</td><td>$codec</td></tr>
			<tr><td>Text</td><td>$text</td></tr>
			</table><br>";

	/*
	 * If 'Test All Voices' or 'Test All Languages' is selected, call testAll to loop through the list
	 */
	if( ($selection == 'voice' && $voice == 'Test All Voices') || (($selection == 'language' && $language == 'Test All Languages')) )
		testAll($selection, $appId, $appKey, $deviceId, $codec, $text);
	/*
	 * Otherwise...
	 *	1. create the request string
	 *	2. execute the http request
	 *	3. process the results
	 */
	else {
		if( $selection == 'voice' )	// We want to make sure we grab the correct language so we build the audio
									// filename properly.
			$language = findLanguageCode($voice);

		$url = buildRequestString($selection, $appId, $appKey, $deviceId, $voice, $language, $codec, $text);
		$m = executeTtsRequest($url);
		processResults($m, $language, $voice);
	}
	echo "</fieldset>";
}
?>

</div>
</center>
</form>
</body>
</html>
