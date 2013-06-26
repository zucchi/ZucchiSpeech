<?php
/**
 * ISpeechToText.php - (http://zucchi.co.uk)
 *
 * @link      http://github.com/zucchi/{PROJECT_NAME} for the canonical source repository
 * @copyright Copyright (c) 2005-2013 Zucchi Limited. (http://zucchi.co.uk)
 * @license   http://zucchi.co.uk/legals/bsd-license New BSD License
 */

namespace ZucchiSpeech\ISpeech\Service;

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
class ISpeechToText
{


    const APIURL = 'http://api.ispeech.org/api/rest?';

    protected $httpClientCredits;

    protected $httpClient;


    protected $appKey = "Insert Your 128-Byte App Key";
    protected $language = "en-gb";
    protected $action= "recognize";
    protected $codec = "audio/x-wav";
    protected $freeform = 1;
    protected $resultsFormat = "json";
    protected $fileName;

    public function __construct($appKey)
    {
        $this->setAppKey($appKey);

        $this->httpClientCredits = new HttpClient(self::APIURL);
        $this->httpClient = new HttpClient(self::APIURL);
    }


    public function transcribe()
    {
        $this->prepareClient();


        $response = $this->httpClient->setMethod(HttpRequest::METHOD_POST);
                      //   ->setRawBody(file_get_contents($this->getFileName()))
                      //   ->setOptions(array('sslverifypeer'=> false, 'sslcapath' => '/etc/ssl/cert'));
                        //->setOptions(array('sslverifypeer'=> false))

        $response = $this->httpClient->send();
        $rawreq = $this->httpClient->getLastRawRequest();
        $content = $response->getContent();

        if($response) {
            $body = $response->getBody();
            $jsonResult  = json_decode($content, true);

            if($jsonResult['result'] == "success"){
                return $jsonResult['text'];
            }
        }

        return "Unable to process speech";
    }

    protected function prepareClient()
    {
        if (!$this->fileName) {
          throw new Exception('You must define a file to use');
        }

        $this->httpClient->setParameterGet(array(
            'apikey' => $this->getAppkey(),
            'action' => $this->action,
            'freeform' => $this->freeform,
            'content-type' => $this->codec,
            'output' => $this->resultsFormat,
            'locale' => $this->language,
            'audio' => base64_encode(file_get_contents($this->getFileName())),

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

    public function getCredits()
	{
        $this->httpClientCredits->setParameterGet(array(
            'apikey' => $this->getAppkey(),
            'action' => 'information',
            'output' => 'json',
        ));

        $response = $this->httpClientCredits->setMethod(HttpRequest::METHOD_GET);

        $response = $this->httpClientCredits->send();
        $content = $response->getContent();
        $body = $response->getBody();
        $jsonResult  = json_decode($content, true);
        return $jsonResult['credits'];
      }

}