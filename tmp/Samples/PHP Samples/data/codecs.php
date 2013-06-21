<?php

/*
 * NOTE: This list is shared between both the Dictation and Text-to-Speech sample PHP scripts.
 *		 Not all codecs are supported between both environments.
 *		 Text-to-Speech supports all codecs listed, however, the NMDP Program currently only supports
 *		 wide-band audio (16Khz) for both US English and non-US English supported ASR languages (see the FAQ for language support).
 * 		 The narrow-band audio (8Khz) options are supported only with US English at this time for ASR requests.
 *
 * Date: May 2, 2011
 */
$codecs = array();
//					GET codec			POST codec								file ext
$codecs[] = array('pcm_16bit_8k', 	'audio/x-wav;codec=pcm;bit=16;rate=8000', 	'pcm');		// narrow-band
$codecs[] = array('pcm_16bit_11k',	'audio/x-wav;codec=pcm;bit=16;rate=11025',	'pcm');		// medium-band
$codecs[] = array('pcm_16bit_16k',	'audio/x-wav;codec=pcm;bit=16;rate=16000',	'pcm');		// wide-band
$codecs[] = array('wav', 			'audio/x-wav;codec=pcm;bit=16;rate=22000', 	'wav');		// wide-band
$codecs[] = array('speex_nb',		'audio/x-speex;rate=8000',					'spx');		// narrow-band
$codecs[] = array('speex_wb', 		'audio/x-speex;rate=16000',					'spx');		// wide-band
$codecs[] = array('amr', 			'audio/amr',								'amr');		// narrow-band
$codecs[] = array('qcelp', 			'audio/qcelp',								'qcp');		// narrow-band
$codecs[] = array('evrc', 			'audio/evrc',								'evr');		// narrow-band


