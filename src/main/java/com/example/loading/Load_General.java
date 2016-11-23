package com.example.loading;

import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreLabel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Created by aning on 16-11-14.
 */
@Configuration
@ComponentScan
public class Load_General {
    public String serializedClassifier = "model/english.all.3class.distsim.crf.ser.gz";
    public static AbstractSequenceClassifier<CoreLabel> classifier;

    @Bean
    public String Load_General() throws Exception {
        classifier = CRFClassifier.getClassifier(serializedClassifier);
        return "load general succeed";
    }

}
