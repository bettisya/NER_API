package com.example.loading;


import banner.eval.BANNER;
import banner.postprocessing.PostProcessor;
import banner.tagging.CRFTagger;
import banner.tokenization.Tokenizer;
import banner.types.Sentence;
import banner.util.SentenceBreaker;
import dragon.nlp.tool.Tagger;
import dragon.nlp.tool.lemmatiser.EngLemmatiser;
import ncbi.chemdner.AbbreviationIdentifier;
import ncbi.chemdner.ParenthesisBalancingPostProcessor;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import javax.xml.stream.XMLStreamException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by aning on 16-5-30.
 */
@Configuration
@ComponentScan
public class Load_Chemical {

        public String configDirectory = "config/banner_JOINT.xml";
        public String dictionaryFilename = "data/dict.txt";
        public String abbreviationDirectory = "AB3P_DIR";
        public String tempDirectory = "TEMP";

        public static AbbreviationIdentifier abbrev;
        public static Tokenizer tokenizer;
        public static PostProcessor postProcessor;
        public static CRFTagger tagger;
        public static SentenceBreaker breaker;
        public static Map<String, String> dict;

        @Bean
        public String Load_Chemical() throws IOException{
            HierarchicalConfiguration config;
            try {
                config = new XMLConfiguration(configDirectory);
            } catch (ConfigurationException e) {
                throw new RuntimeException(e);
            }

            EngLemmatiser lemmatiser = BANNER.getLemmatiser(config);
            Tagger posTagger = BANNER.getPosTagger(config);
            tokenizer = BANNER.getTokenizer(config);
            postProcessor = new ParenthesisBalancingPostProcessor();

            HierarchicalConfiguration localConfig = config.configurationAt(BANNER.class.getPackage().getName());
            String modelFilename = localConfig.getString("modelFilename");
            System.out.println("modelFilename = " + modelFilename);

            tagger = CRFTagger.load(new File(modelFilename), lemmatiser, posTagger, null);
            abbrev = new AbbreviationIdentifier("./identify_abbr", abbreviationDirectory, tempDirectory, 1000);
            breaker = new SentenceBreaker();
            dict = loadDictionary(dictionaryFilename);
            return "success loaded";
        }


    private static Map<String, String> loadDictionary(String filename) throws IOException {
        Map<String, String> dict = new HashMap<>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(filename));
            String line = reader.readLine();
            while (line != null) {
                String[] fields = line.split("\t");
                String text = fields[0];
                String conceptId = fields[1];
                dict.put(text, conceptId);
                line = reader.readLine();
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        return dict;
    }


}
