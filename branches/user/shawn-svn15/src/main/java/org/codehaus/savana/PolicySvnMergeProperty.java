package org.codehaus.savana;

import org.apache.commons.lang.BooleanUtils;

import java.util.Properties;

/**
 * Since Savana keeps track of merge history within user branches, the
 * "svn:mergeinfo" property added in Subversion 1.5 isn't strictly required,
 * and with the amount of merging that can happen in a project with
 * lost of user branch and promote activity the changes to "svn:mergeinfo"
 * can get distracting when viewing changesets or synchronizing changes.
 * <p>
 * Turning on this policy tells Savana to delete "svn:mergeinfo" at the
 * top level after every Synchronize and Promote operation, simulating
 * the behavior of Subversion 1.4.
 */
public class PolicySvnMergeProperty {

    private final Properties _properties;

    public PolicySvnMergeProperty(Properties properties) {
        _properties = properties;
    }

    public boolean shouldDeleteSvnMergeProperty() {
        return BooleanUtils.toBoolean(_properties.getProperty("svnmergeproperty.delete"));
    }
}
