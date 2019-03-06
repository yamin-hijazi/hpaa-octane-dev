/*
 * Certain versions of software and/or documents ("Material") accessible here may contain branding from
 * Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 * the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 * and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 * marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * (c) Copyright 2012-2019 Micro Focus or one of its affiliates.
 *
 * The only warranties for products and services of Micro Focus and its affiliates
 * and licensors ("Micro Focus") are set forth in the express warranty statements
 * accompanying such products and services. Nothing herein should be construed as
 * constituting an additional warranty. Micro Focus shall not be liable for technical
 * or editorial errors or omissions contained herein.
 * The information contained herein is subject to change without notice.
 * ___________________________________________________________________
 */

package com.microfocus.application.automation.tools.octane.tests.impl;

import hudson.FilePath;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class ObjectStreamIterator<E> implements Iterator<E> {
    private static Logger logger = LogManager.getLogger(ObjectStreamIterator.class);

    private ObjectInputStream ois;
    private FilePath filePath;
    private E next;

    public ObjectStreamIterator(FilePath filePath) throws IOException, InterruptedException {
        this.filePath = filePath;
        this.ois = new ObjectInputStream(new BufferedInputStream(filePath.read()));
    }

    @Override
    public boolean hasNext() {
        if (next != null) {
            return true;
        }
        try {
            next = (E) ois.readObject();
            return true;
        } catch (Throwable e) {
            try {
                ois.close();
            } catch (IOException ioe) {
                logger.error("Failed to close the stream", ioe); // NON-NLS
            }

            try {
                filePath.delete();
            } catch (Exception ex) {
                logger.error("Failed to delete the filePath " + filePath.getRemote(), ex); // NON-NLS
            }
            return false;
        }
    }

    @Override
    public E next() {
        if (hasNext()) {
            E value = next;
            next = null;
            return value;
        } else {
            throw new NoSuchElementException();
        }
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
