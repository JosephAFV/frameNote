package com.framenote.domain;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class BusinessRulesTest {
 @Test void validVideoTime(){ assertDoesNotThrow(()->BusinessRules.validTimestamp(15000,30000)); assertThrows(IllegalArgumentException.class,()->BusinessRules.validTimestamp(45000,30000));}
 @Test void quota(){assertDoesNotThrow(()->BusinessRules.validQuota(200,0,50,250));assertThrows(IllegalArgumentException.class,()->BusinessRules.validQuota(210,0,80,250));}
 @Test void coordinates(){assertDoesNotThrow(()->BusinessRules.validCoordinates(0.4,0.7));assertThrows(IllegalArgumentException.class,()->BusinessRules.validCoordinates(2.0,0.4));}
 @Test void states(){assertDoesNotThrow(()->BusinessRules.validFeedbackTransition("PENDING","RESOLVED"));assertThrows(IllegalArgumentException.class,()->BusinessRules.validFeedbackTransition("PENDING","REOPENED"));}
}
