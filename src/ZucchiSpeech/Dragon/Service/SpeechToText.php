<?php
/**
 * SpeechToText.php - (http://zucchi.co.uk) 
 *
 * @link      http://github.com/zucchi/{PROJECT_NAME} for the canonical source repository
 * @copyright Copyright (c) 2005-2013 Zucchi Limited. (http://zucchi.co.uk)
 * @license   http://zucchi.co.uk/legals/bsd-license New BSD License
 */

namespace ZucchiSpeech\Dragon\Service;

use Zend\Http\Client as HttpClient;
use Zend\Http\Client\Adapter\Curl as Curl;
use Zend\Http\Request as HttpRequest;

/**
 * SpeechToText
 *
 * Description of class
 *
 * @author Matt Cockayne <matt@zucchi.co.uk>
 * @package ZucchiNuance\Dragon\Service
 * @subpackage 
 * @category 
 */
class SpeechToText 
{


    const NUANCE_API = 'https://dictation.nuancemobility.net/NMDPAsrCmdServlet/dictation';

    protected $httpClient;

    protected $appId = "Insert Your App Id";
    protected $appKey = "Insert Your 128-Byte App Key";
    protected $deviceId = "0000";
    protected $language = "en_us";
    protected $codec = "audio/x-wav;codec=pcm;bit=16;rate=16000";
    protected $languageModel = "Dictation";
    protected $resultsFormat = "application/xml";

    protected $fileName;

    protected $availableLanguages = array(
        'en_us' => 'US English',
        'en_gb' => 'UK English',
        'fr_fr' => 'French',
        'it_it' => 'Italian',
        'de_de' => 'German',
        'es_es' => 'EU Spanish',
        'ja_jp' => 'Japanese',
    );

    protected $codecs = array(
        'pcm_16bit_8k' =>  array('mime' => 'audio/x-wav;codec=pcm;bit=16;rate=8000', 'ext' => 'pcm'),		// narrow-band
        'pcm_16bit_11k' => array('mime' => 'audio/x-wav;codec=pcm;bit=16;rate=11025', 'ext' => 'pcm'),		// medium-band
        'pcm_16bit_16k' => array('mime' => 'audio/x-wav;codec=pcm;bit=16;rate=16000', 'ext' => 'pcm'),		// wide-band
        'wav' =>           array('mime' => 'audio/x-wav;codec=pcm;bit=16;rate=22000', 'ext' => 'wav'),		// wide-band
        'speex_nb' =>      array('mime' => 'audio/x-speex;rate=8000', 'ext' => 'spx'),		// narrow-band
        'speex_wb' =>      array('mime' => 'audio/x-speex;rate=16000', 'ext' => 'spx'),		// wide-band
        'amr' =>           array('mime' => 'audio/amr', 'ext' => 'amr'),		// narrow-band
        'qcelp' =>         array('mime' => 'audio/qcelp', 'ext' => 'qcp'),		// narrow-band
        'evrc' =>          array('mime' => 'audio/evrc', 'ext' => 'evr'),		// narrow-band
    );

    public function __construct($appId, $appKey)
    {
        $this->setAppId($appId);
        $this->setAppKey($appKey);

        $this->httpClient = new HttpClient(self::NUANCE_API);
    }


    public function transcribe()
    {
        $this->prepareClient();


        $response = $this->httpClient->setMethod(HttpRequest::METHOD_POST)
                         ->setRawBody(File_get_contents($this->getFileName()))
                         ->setOptions(array('sslverifypeer'=> false, 'sslcapath' => '/etc/ssl/cert'));
                       // ->setOptions(array('sslverifypeer'=> false));

        $response = $this->httpClient->send();

        $voiceResults = explode(":\n",$response->getContent());
        return $voiceResults[0];
        //echo $this->httpClient->getLastRawRequest();
        //echo PHP_EOL . $response->getBody() . PHP_EOL;

    }

    protected function prepareClient()
    {
        if (!$this->fileName) {
            throw new Exception('You must define a file to use');
        }


        $len = filesize($this->fileName);
        // set headers
        $headers = array(
            'Content-Type' => "audio/x-wav;codec=pcm;bit=16;rate=16000",
            'Content-Language' => $this->language,
            'Accept-Language' => $this->language,
            'Accept' => $this->resultsFormat,
            'Accept-Topic' => $this->languageModel,
            'Content-Length' => $len,
            //'Transfer-Encoding'=> 'chunked',
        );

        $this->httpClient->setHeaders($headers);



        $this->httpClient->setParameterGet(array(
            'appId' => $this->getAppId(),
            'appKey' => $this->getAppKey(),
            'id' => $this->getDeviceId(),
        ));

    }

    /**
     * @param mixed $file
     */
    public function setFileName($fileName)
    {
        $this->fileName = $fileName;
    }

    /**
     * @return mixed
     */
    public function getFileName()
    {
        return $this->fileName;
    }

    /**
     * @param string $appId
     */
    public function setAppId($appId)
    {
        $this->appId = $appId;
    }

    /**
     * @return string
     */
    public function getAppId()
    {
        return $this->appId;
    }

    /**
     * @param string $appKey
     */
    public function setAppKey($appKey)
    {
        $this->appKey = $appKey;
    }

    /**
     * @return string
     */
    public function getAppKey()
    {
        return $this->appKey;
    }

    /**
     * @param string $codec
     */
    public function setCodec($codec)
    {
        if (!isset($this->codecs[$codec])) {
            throw new Exception('You must set a valid codec');
        }

        $this->codec = $codec;
    }

    /**
     * @return string
     */
    public function getCodec()
    {
        return $this->codec;
    }

    /**
     * @param string $deviceId
     */
    public function setDeviceId($deviceId)
    {
        $this->deviceId = $deviceId;
    }

    /**
     * @return string
     */
    public function getDeviceId()
    {
        return $this->deviceId;
    }

    /**
     * @param string $language
     */
    public function setLanguage($language)
    {
        $this->language = $language;
    }

    /**
     * @return string
     */
    public function getLanguage()
    {
        return $this->language;
    }

    /**
     * @param string $languageModel
     */
    public function setLanguageModel($languageModel)
    {
        $this->languageModel = $languageModel;
    }

    /**
     * @return string
     */
    public function getLanguageModel()
    {
        return $this->languageModel;
    }

    /**
     * @param string $resultsFormat
     */
    public function setResultsFormat($resultsFormat)
    {
        $this->resultsFormat = $resultsFormat;
    }

    /**
     * @return string
     */
    public function getResultsFormat()
    {
        return $this->resultsFormat;
    }




}