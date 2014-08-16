package com.github.junitcharacterization;

import com.github.junitcharacterization.rules.CaptureVerifier;
import com.github.junitcharacterization.rules.FileCreateRule;
import com.github.junitcharacterization.rules.FileDeleteRule;
import com.github.junitcharacterization.rules.FileOutputCapture;
import com.github.junitcharacterization.rules.StreamOutputCapture;
import org.junit.rules.TestRule;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class CharacterizationBuilder {

    final public static String ENV_NAME_FOR_RECORDING = "pinchpoint";
    final private static String DEFAULT_FOLDER = System.getProperty("java.io.tmpdir");
    final private String DEFAULT_FILENAME;
    final private RulesBuilder rulesBuilder;

    public CharacterizationBuilder(Class<?> clazz) {
        this.DEFAULT_FILENAME = clazz.getCanonicalName() + ".txt";
        this.rulesBuilder = withDefaultRules();
    }

    private RulesBuilder withDefaultRules() {
        return new RulesBuilder()
                .clearOutputBeforeCapture()
                .inFolder(DEFAULT_FOLDER)
                .withFilename(DEFAULT_FILENAME);
    }

    public CharacterizationBuilder appendMode() {
        this.rulesBuilder.appendToExistingFile();
        return this;
    }

    public CharacterizationRule build() {
        return new CharacterizationRule(rulesBuilder.build());
    }

    public static class RulesBuilder {

        final private static Logger log = Logger.getLogger(RulesBuilder.class.getSimpleName());
        private List<TestRule> rules = new ArrayList<>();
        private File folder;
        private String filename;
        private Boolean deleteExistingFile;

        public RulesBuilder inFolder(String folder) {
            this.folder = new File(folder);
            return this;
        }

        public RulesBuilder withFilename(String filename) {
            this.filename = filename;
            return this;
        }

        public RulesBuilder appendToExistingFile() {
            this.deleteExistingFile = false;
            return this;
        }

        public RulesBuilder clearOutputBeforeCapture() {
            this.deleteExistingFile = true;
            return this;
        }

        public List<TestRule> build() {
            if (folder == null || "".equals(folder)) {
                throw new IllegalArgumentException("Folder cannot be empty");
            }

            if (filename == null || "".equals(filename)) {
                throw new IllegalArgumentException("Filename cannot be empty");
            }

            final ByteArrayOutputStream capturedStream = new ByteArrayOutputStream();
            final File outputFile = new File(folder, filename);

            String env = System.getProperty(ENV_NAME_FOR_RECORDING);
            if (env != null) {
                log.warning("RECORDING MODE! Output to file [filename="+outputFile.toURI()+"]");
                rules.add(fileHandlingModeRule(outputFile));
                rules.add(new FileOutputCapture(outputFile, capturedStream));
            } else {
                rules.add(new StreamOutputCapture(capturedStream));
                rules.add(new CaptureVerifier(outputFile, capturedStream));
            }

            return rules;
        }

        private TestRule fileHandlingModeRule(File outputFile) {
            if (deleteExistingFile) {
                return new FileDeleteRule(outputFile);
            } else {
                return new FileCreateRule(outputFile);
            }
        }
    }
}