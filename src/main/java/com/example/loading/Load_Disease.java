package com.example.loading;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;

import dnorm.core.DiseaseNameAnalyzer;
import dnorm.core.Lexicon;
import dnorm.core.MEDICLexiconLoader;
import dnorm.core.SynonymTrainer;
import dnorm.core.SynonymTrainer.LookupResult;
import dnorm.types.FullRankSynonymMatrix;
import dnorm.util.AbbreviationIdentifier;
import dnorm.util.AbbreviationResolver;
import dnorm.util.PubtatorReader;
import dnorm.util.PubtatorReader.Abstract;
import dragon.nlp.tool.Tagger;
import dragon.nlp.tool.lemmatiser.EngLemmatiser;
import banner.eval.BANNER;
import banner.postprocessing.PostProcessor;
import banner.tagging.CRFTagger;
import banner.tagging.dictionary.DictionaryTagger;
import banner.tokenization.Tokenizer;
import banner.types.Mention;
import banner.types.Sentence;
import banner.types.Mention.MentionType;
import banner.util.RankedList;
import dragon.util.EnvVariable;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.IOException;

/**
 * Created by aning on 16-6-1.
 */
@Configuration
@ComponentScan
public class Load_Disease {
    public String configurationFilename = "config/banner_BC5CDR_UMLS2013AA.xml";
    public String lexiconFilename = "data/CTD_diseases-2015-06-04.tsv";
    public String matrixFilename = "output/simmatrix_BC5CDR_e4.bin";
    public String abbreviationDirectory = "AB3P_DIR";
    public String tempDirectory = "TEMP";

    public static AbbreviationIdentifier abbrev;
    public static CRFTagger tagger;
    public static Tokenizer tokenizer;
    public static PostProcessor postProcessor;
    public static SynonymTrainer syn;

    @Bean
    public String Load_Disease() throws ConfigurationException, IOException {

        DiseaseNameAnalyzer analyzer = DiseaseNameAnalyzer.getDiseaseNameAnalyzer(true, true, false, true);
        Lexicon lex = new Lexicon(analyzer);
        MEDICLexiconLoader loader = new MEDICLexiconLoader();
        loader.loadLexicon(lex, lexiconFilename);
        lex.prepare();

        FullRankSynonymMatrix matrix = FullRankSynonymMatrix.load(new File(matrixFilename));
        syn = new SynonymTrainer(lex, matrix, 1000);

        HierarchicalConfiguration config = new XMLConfiguration(configurationFilename);
        EnvVariable.setDragonHome(".");
        EnvVariable.setCharSet("US-ASCII");
        EngLemmatiser lemmatiser = BANNER.getLemmatiser(config);
        Tagger posTagger = BANNER.getPosTagger(config);
        HierarchicalConfiguration localConfig = config.configurationAt(BANNER.class.getPackage().getName());
        String modelFilename = localConfig.getString("modelFilename");
        tokenizer = BANNER.getTokenizer(config);
        postProcessor = BANNER.getPostProcessor(config);
        tagger = CRFTagger.load(new File(modelFilename), lemmatiser, posTagger, null);

        abbrev = new AbbreviationIdentifier("./identify_abbr", abbreviationDirectory, tempDirectory, 1000);

        return "success loaded";
    }

}
