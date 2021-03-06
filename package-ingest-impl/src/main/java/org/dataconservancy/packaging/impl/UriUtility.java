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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FilenameUtils;

/**
 * A utility class for doing URI-related work for verification and uniform File handling
 *
 * @author bbrosius@jhu.edu
 */
public abstract class UriUtility {

    private static final String ERR_RESOLVE_BAGURI = "Unable to resolve bag uri %s against base directory %s: ";

    static String BAG_URI_SCHEME = "bag";

    static String FILE_URI_SCHEME = "file";

    /**
     * Determine if a URL is a URL using HTTP or HTTPS protocols
     *
     * @param toCheck The URI to check
     * @return true if the URI is a URL with a non-empty host and uses either the http or https protocol
     */
    public static boolean isHttpUrl(final URI toCheck) {
        return toCheck.getHost() != null &&
                (toCheck.getScheme().equals("http") ||
                        toCheck.getScheme().equals("https"));
    }

    /**
     * Determine if a string is a URL with HTTP or HTTPS protocols
     *
     * @param toCheck the string to check
     * @return true if the string is a URL with http or https protocols
     */
    public static boolean isHttpUrl(final String toCheck) {
        try {
            return isHttpUrl(new URI(toCheck));
        } catch (final URISyntaxException e) {
            return false;
        }
    }

    /**
     * Determine if a URI is resolvable. Currently this means the URI is a valid URL
     *
     * @param toCheck the URI to check
     * @return true if resolvable, false if not
     */
    public static boolean isResolvable(final URI toCheck) {
        try {
            toCheck.toURL();
        } catch (final MalformedURLException e) {
            return false;
        }

        return true;
    }

    /**
     * Create a URI string for a file, ensuring that it has 3 slashes to meet File URL specifications
     *
     * @param file The file to check. This doesn't have to be an actual existing file
     * @param basedir The directory to make the file URI relative to. Can be null. If not null, the basedir must be in
     *        the path of the file parameter, or an exception will be thrown
     * @return A string representing the URI to the file on the local disk.
     * @throws URISyntaxException if there is an error in the URI syntax
     */
    public static URI makeFileUriString(final File file, final File basedir) throws URISyntaxException {
        final File dir = basedir == null ? new File(".") : basedir;

        Path relativePath = file.toPath();

        if (relativePath.startsWith(dir.toPath())) {
            relativePath = dir.toPath().relativize(file.toPath());
        }

        String path = FilenameUtils.separatorsToUnix(relativePath.toString());

        // Remove leading slashes from the path
        path = path.replaceFirst("^\\/*", "");

        return new URI("file", null, "///" + path, null);
    }

    /**
     * Create a URI string for a file in a BagIt bag,
     *
     * @param file The file to check. This doesn't have to be an actual existing file
     * @param basedir The directory to make the file URI relative to. Can be null. If not null, the basedir must be in
     *        the path of the file parameter, or an exception will be thrown
     * @return A string representing the URI to the file on the local disk.
     * @throws URISyntaxException if there is an error in the URI syntax
     */
    public static URI makeBagUriString(final File file, final File basedir) throws URISyntaxException {
        final File dir = basedir == null ? new File(".") : basedir;

        Path relativePath = file.toPath();

        if (relativePath.startsWith(dir.toPath())) {
            relativePath = dir.toPath().relativize(file.toPath());
        }

        String path = FilenameUtils.separatorsToUnix(relativePath.toString());
        if (relativePath.getNameCount() > 1) {

            final Path uriAuthority = relativePath.getName(0);
            final Path uriPath = relativePath.subpath(1, relativePath.getNameCount());
            path = FilenameUtils.separatorsToUnix(uriPath.toString());
            if (!uriPath.isAbsolute()) {
                path = "/" + path;
            }

            return new URI(BAG_URI_SCHEME, uriAuthority.toString(), path, null, null);
        }

        return new URI(BAG_URI_SCHEME, path, null, null, null);
    }

    /**
     * Resolves the supplied {@code bag://} URI against a platform-specific base directory. This method is used to
     * resolve resources in a bag to a platform-specific {@code Path} used by the caller to access the content of the
     * resource.
     * <p>
     * Example usage: Given a bag that contains a resource identified by the URI {@code bag://my-bag/data/bar}, and
     * the bag has been exploded into the directory {@code /tmp/foo/my-bag} (where the bag payload directory is
     * located at {@code /tmp/foo/my-bag/data}) then the base directory of the bag is {@code /tmp/foo}. If the caller
     * wishes to resolve the URI {@code bag://my-bag/data/bar}, they would invoke this method:
     * </p>
     * <pre>
     *     Path result = UriUtility.resolveBagUri(Paths.get("/tmp/foo"), new URI("bag://my-bag/data/bar"));
     *     assert Paths.get("/tmp/foo/my-bag/data/bar").equals(result);
     * </pre>
     * <p>
     * The base directory does not need to exist. This implementation will {@link Path#normalize() normalize} the
     * supplied directory.
     * </p>
     * <p>
     * The {@code bag://} URI is converted to a path by concatenating the authority portion of the URI with the path
     * portion.
     * </p>
     * <p>
     * If the supplied {@code bagUri} is <em>not</em> a URI with the {@code bag} scheme, an
     * {@code IllegalArgumentException} is thrown.
     * </p>
     *
     * @param baseDir the base directory that contains the bag
     * @param bagUri a URI identifying a resource in a bag
     * @return a platform-specific {@code Path}, used to access the contents of the resource identified by
     *         {@code bagUri}
     * @throws IllegalArgumentException if the supplied bagUri is null or empty, if {@code baseDir} is null, if
     *         {@code bagUri} does not have scheme {@code bag}
     * @throws RuntimeException if the supplied base directory cannot be normalized
     */
    public static Path resolveBagUri(final Path baseDir, final URI bagUri) {
        if (bagUri == null) {
            throw new IllegalArgumentException(
                    String.format(ERR_RESOLVE_BAGURI + "bag uri was null.", "null", baseDir));
        }

        if (!bagUri.getScheme().equals(BAG_URI_SCHEME)) {
            throw new IllegalArgumentException(
                    String.format(ERR_RESOLVE_BAGURI + "bag uri had incorrect scheme.", bagUri, baseDir));
        }

        if (baseDir == null) {
            throw new IllegalArgumentException(
                    String.format(ERR_RESOLVE_BAGURI + "base directory was null", bagUri, "null"));
        }

        // normalize the base directory path
        final Path originalDir = baseDir;
        final Path normalizedDir = baseDir.normalize();

        if (normalizedDir == null) {
            throw new RuntimeException(String.format(ERR_RESOLVE_BAGURI +
                    "failed to normalize the base directory.", bagUri, originalDir));
        }

        final Path bagPath = Paths.get(bagUri.getAuthority(), bagUri.getPath());

        return normalizedDir.resolve(bagPath);
    }

    /**
     * Returns true if the scheme for {@code uri} is equal to {@code bag}, otherwise it returns false.
     *
     * @param uri the uri
     * @return true if the scheme of the URI equals {@code bag}
     */
    public static boolean isBagUri(final URI uri) {
        if (uri == null) {
            return false;
        }

        return BAG_URI_SCHEME.equals(uri.getScheme());
    }

}