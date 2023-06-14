package org.jboss.pnc.dingrogu.common;

import com.fasterxml.jackson.databind.JsonNode;
import org.jboss.pnc.api.constants.HttpHeaders;
import org.jboss.pnc.api.dto.Request;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.compile;

/**
 * Parse a nice name from git repo URL.
 *
 * Git can use four major protocols to transfer data: Local, HTTP, Secure Shell (SSH) and Git.
 * https://git-scm.com/book/ch4-1.html
 *
 * @author Jakub Senko
 */
public class GitUrlParser {
    public static class GitUrlData {
        private String domain;
        private String organization;
        private String repository;

        public String getDomain() {
            return domain;
        }

        public void setDomain(String domain) {
            this.domain = domain;
        }

        public String getOrganization() {
            return organization;
        }

        public void setOrganization(String organization) {
            this.organization = organization;
        }

        public String getRepository() {
            return repository;
        }

        public void setRepository(String repository) {
            this.repository = repository;
        }

        @Override
        public String toString() {
            return "GitUrlData{" + "domain='" + domain + '\'' + ", organization='" + organization + '\''
                    + ", repository='" + repository + '\'' + '}';
        }
    }

    private static final String PROTOCOL_HTTP_HTTPS = "(https?://)";

    private static final String PROTOCOL_GIT = "(git://)";

    private static final String PROTOCOL_GIT_SSH = "(git\\+ssh://)";

    private static final String PROTOCOL_SSH = "(ssh://)";

    private static final String PROTOCOLS = "(" + PROTOCOL_HTTP_HTTPS + "|" + PROTOCOL_GIT + "|" + PROTOCOL_GIT_SSH
            + "|" + PROTOCOL_SSH + ")";

    private static final String USER = "(?<user>[^@/ \\n]+?@)";

    private static final String DOMAIN = "(?<domain>(\\.?[0-9a-z\\-_\\~]+)+)";

    private static final String PORT = "(?<port>:[0-9]+)";

    private static final String PATH = ":?(?<path>/?([^:/\\s]+/)*[^:/\\s]+/?)";

    /**
     * git clone git://github.com/foo/foo.git git clone ssh://user@server/project.git git clone
     * git@github.com:foo/foo.git git clone https://foo.com/project.git
     */
    private static final Pattern REMOTE = compile(
            "^" + PROTOCOLS + "?" + USER + "?" + DOMAIN + PORT + "?" + PATH + "?$");

    public static GitUrlData parseToObject(String url) {

        Matcher matcher = REMOTE.matcher(url);
        if (!matcher.matches()) {
            return null;
        }
        String domain = matcher.group("domain");
        String path = matcher.group("path");

        if (domain == null || path == null) {
            return null;
        }

        GitUrlData gitData = new GitUrlData();
        gitData.setDomain(domain);

        String[] pathParts = path.split("/");
        int last = pathParts.length - 1;
        gitData.setRepository(
                pathParts[last].endsWith(".git") ? pathParts[last].substring(0, pathParts[last].length() - 4)
                        : pathParts[last]);

        int previousToLast = pathParts.length - 2;

        // If organization part is 'gerrit', ignore it since that git repository has no organization

        if (previousToLast >= 0 && !pathParts[previousToLast].equals("gerrit")) {
            gitData.setOrganization(pathParts[previousToLast]);
        }
        return gitData;
    }

    public static String generateInternalGitRepoName(String externalUrl) {
        GitUrlData urlData = GitUrlParser.parseToObject(externalUrl);
        if (urlData != null) {
            String organization = urlData.getOrganization();
            String repository = urlData.getRepository();
            if (organization != null && !organization.isEmpty()) {
                return organization + "/" + repository;
            } else {
                return repository;
            }
        } else {
            String[] a = externalUrl.split("/");
            if (a.length != 0) {
                return a[a.length - 1];
            } else {
                return null;
            }
        }
    }
}
