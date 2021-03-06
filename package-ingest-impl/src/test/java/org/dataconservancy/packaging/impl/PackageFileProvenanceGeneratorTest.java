/*
 * Copyright 2016 Johns Hopkins University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dataconservancy.packaging.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.dataconservancy.packaging.ingest.PackagedResource;

import org.junit.Before;
import org.junit.Test;

/**
 * @author bbrosius@jhu.edu
 */
public class PackageFileProvenanceGeneratorTest {

    private PackageFileProvenanceGenerator underTest;

    private File packageFile;

    private Map<String, String> testURIMap;

    @Before
    public void setup() {
        underTest = new PackageFileProvenanceGenerator();

        // Since we don't actually need the file to be a package file we'll just use a test resource.
        final URL packageUrl = PackageFileAnalyzerTest.class.getResource("/test_pkg/bagit.txt");
        packageFile = new File(packageUrl.getPath());

        testURIMap = new HashMap<>();
        testURIMap.put("uri:foo", "bag://file1");
        testURIMap.put("uri:bar", "bag://file2");
    }

    @Test
    public void testProvenanceGeneration() {
        final PackagedResource packageResource = underTest.generatePackageProvenance(packageFile, testURIMap);

        assertNotNull(packageResource);

        assertEquals(packageFile.toURI(), packageResource.getURI());
        assertEquals(PackagedResource.Type.NONRDFSOURCE, packageResource.getType());
        assertTrue(packageResource.getChildren().isEmpty());
        assertNotNull(packageResource.getBody());

        assertNotNull(packageResource.getDescription());

        final PackagedResource provenanceResource = packageResource.getDescription();
        assertNotNull(provenanceResource.getBody());
        assertNull(provenanceResource.getDescription());
        assertTrue(provenanceResource.getChildren().isEmpty());
        assertEquals(PackagedResource.Type.RDFSOURCE, provenanceResource.getType());
    }
}
