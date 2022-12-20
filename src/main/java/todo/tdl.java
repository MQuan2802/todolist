package todo;

import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;

import com.mysql.cj.xdevapi.AddStatement;

import org.springframework.http.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.sql.*;
import java.util.*;
import java.util.concurrent.Callable;

import javax.print.DocFlavor.STRING;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.concurrent.*;
import java.net.URLDecoder;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.Charset;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;
import java.util.Objects;
import org.springframework.ui.Model;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.*;
import java.io.*;

@Controller
public class tdl {

    final static String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    final static String DB_URL = "jdbc:mysql://localhost:3306/todolist?useUnicode=yes&characterEncoding=UTF-8";
    final static String USER = "authenticationuser";
    final static String PASS = "436553";

    public static boolean modifyLastModheader(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        long lastModifiedFromBrowser = req.getDateHeader("If-Modified-Since");
        long lastModifiedFromServer = getLastModifiedMillis();

        if (lastModifiedFromBrowser != -1 &&
                lastModifiedFromServer <= lastModifiedFromBrowser) {
            // setting 304 and returning with empty body
            resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            return false;
        }
        resp.addHeader("Cache-Control", "no-cache");
        resp.addDateHeader("Last-Modified", lastModifiedFromServer);
        return true;
    }

    private static long getLastModifiedMillis() throws Exception {
        // Using hard coded value, in real scenario there should be for example
        // last modified date of this servlet or of the underlying resource
        final String sqlgetmodDateT = "select * from lastmodified";
        PreparedStatement modDateTstmt = null;
        ResultSet rs = null;
        Connection conn = null;
        try {
            Class.forName(JDBC_DRIVER);
            conn = DriverManager.getConnection(DB_URL, USER, PASS);
            modDateTstmt = conn.prepareStatement(sqlgetmodDateT);
            rs = modDateTstmt.executeQuery();
            boolean hasnext = rs.next();
            if (hasnext) {
                /*
                 * final DateTimeFormatter format =
                 * DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
                 * ZonedDateTime zdtWithZoneOffset = ZonedDateTime.parse(rs.getString("DateT"),
                 * format.withZone(ZoneId.of("GMT")));
                 * 
                 * return zdtWithZoneOffset.toInstant().toEpochMilli();
                 */
                long millisec = Long.parseLong(rs.getString("DateT"));
                return millisec;
            }

        } catch (SQLException se) {
            try {
                if (conn != null)
                    conn.rollback();
            } catch (SQLException se2) {
                se2.printStackTrace();
            }

        } catch (Exception E) {
            E.printStackTrace();
        } finally {
            if (modDateTstmt != null) {
                try {
                    modDateTstmt.close();
                } catch (SQLException SE3) {
                    SE3.printStackTrace();
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException SE3) {
                    SE3.printStackTrace();
                }
            }
        }
        return 0;

    }

    @RequestMapping(value = "/todolist/{taskname}", method = RequestMethod.POST, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String updateresponse(@PathVariable("taskname") String taskname,
            @RequestBody MultiValueMap<String, String> reqparam,
            RedirectAttributes redirectAttrs) {
        String rturn = "";
        if (taskname.compareTo("update") == 0) {
            redirectAttrs.addFlashAttribute("update", reqparam.get("update").get(0));
            redirectAttrs.addFlashAttribute("oldtask", reqparam.get("oldtask").get(0));
            rturn = "redirect:redirectupdate";
        }
        if (taskname.compareTo("addtask") == 0) {
            redirectAttrs.addFlashAttribute("addmoretask", reqparam.get("addmoretask").get(0));
            rturn = "redirect:redirectadd";
        }
        return rturn;
    }

    @RequestMapping(value = "/todolist/redirectupdate", method = RequestMethod.GET)
    public Callable<ResponseEntity<String>> updateredirect(Model model, HttpServletResponse resp)
            throws InterruptedException, UnsupportedEncodingException, URISyntaxException, Exception {
        String oldtask = (String) model.asMap().get("oldtask");
        String update = (String) model.asMap().get("update");
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Callable<String> clbupdate = new updatetask(oldtask, update);
        Future<String> stringFuture = executorService.submit(clbupdate);
        while (!stringFuture.isDone() && !stringFuture.isCancelled()) {
            Thread.sleep(200);
            System.out.println("Waiting for task completion...");
        }
        executorService.shutdown();
        System.out.println("task completed");
        resp.addHeader("Cache-Control", "no-cache");
        resp.addDateHeader("Last-Modified", getLastModifiedMillis());
        Callable<ResponseEntity<String>> clb = new test(null);
        return clb;
    }

    @RequestMapping(value = "/todolist/delete/{delete}", method = RequestMethod.GET)
    @ResponseBody
    public Callable<ResponseEntity<String>> deleteresponse(@PathVariable String delete, HttpServletResponse resp)
            throws InterruptedException, Exception {

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Callable<String> clbupdate = new deletetask(delete);
        Future<String> stringFuture = executorService.submit(clbupdate);
        while (!stringFuture.isDone() && !stringFuture.isCancelled()) {
            Thread.sleep(200);
            System.out.println("Waiting for task completion...");

        }
        System.out.println("task completed");
        executorService.shutdown();
        resp.addHeader("Cache-Control", "no-cache");
        resp.addDateHeader("Last-Modified", getLastModifiedMillis());
        Callable<ResponseEntity<String>> clb = new test(null);
        return clb;
    }

    @RequestMapping(value = "/todolist", method = RequestMethod.GET)
    public Callable<ResponseEntity<String>> response(HttpServletRequest req,
            HttpServletResponse resp, @RequestHeader Map<String, String> header)
            throws Exception {
        if (!modifyLastModheader(req, resp))
            return null;

        Callable<ResponseEntity<String>> clb = new test(null);
        return clb;
    }

    @RequestMapping(value = "/todolist/redirectadd", method = RequestMethod.GET)
    public Callable<ResponseEntity<String>> addredirect(Model model, HttpServletResponse resp)
            throws InterruptedException, UnsupportedEncodingException, URISyntaxException, Exception {
        resp.addHeader("Cache-Control", "no-cache");
        resp.addDateHeader("Last-Modified", getLastModifiedMillis());
        Callable<ResponseEntity<String>> clb = new test((String) model.asMap().get("addmoretask"));
        return clb;
    }

}

final class test implements Callable<ResponseEntity<String>> {
    private String variable;

    public test(String clgt) {
        this.variable = clgt;
    }

    @Override
    public ResponseEntity<String> call() throws Exception {

        final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
        final String DB_URL = "jdbc:mysql://localhost:3306/todolist?useUnicode=yes&characterEncoding=UTF-8";
        final String USER = "authenticationuser";
        final String PASS = "436553";
        final String sqladd = "insert into tdlist (task) values (N'" + variable + "')";
        final String sql = "select * from tdlist";
        Connection conn = null;
        PreparedStatement stmt = null;
        PreparedStatement addstmt = null;
        ResultSet rs = null;

        String rturn = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\"\"http://www.w3.org/TR/html4/strict.dtd\">\n"
                + "<html>\n" +
                "<head>\n" +
                " <style type=\"text/css\"> \n" +
                "form {display: inline-block; //Or display: inline; }\n" +
                "p {display:inline-block}\n"
                + "</style>\n" +
                "<meta charset=\"utf-8\">\n" +
                "</head>\n" +
                "<body>\n"
                + "<h1>TASK LIST 2022</h1>\n" +
                "<form action=\"/demo/ahihi/todolist/addtask\" method=\"post\" accept-charset=\"utf-8\">" +
                "<input type=\"text\" id=\"addtask\" name=\"addmoretask\">" +
                "<input type=\"submit\" value=\"addtask\">" +
                "</form><br>";

        try {
            Class.forName(JDBC_DRIVER);
            conn = DriverManager.getConnection(DB_URL, USER, PASS);
            conn.setAutoCommit(false);
            stmt = conn.prepareStatement(sql);
            if (variable != null) {
                if (!variable.isEmpty()) {
                    LocalDateTime now = LocalDateTime.now();
                    ZonedDateTime zonedDateTime = now.atZone(ZoneId.of("GMT"));
                    long millisec = zonedDateTime.toInstant().toEpochMilli();
                    final String sqlupdatemodDateT = "update  lastmodified set DateT=" + millisec;
                    PreparedStatement updateDateT = null;
                    addstmt = conn.prepareStatement(sqladd);
                    updateDateT = conn.prepareStatement(sqlupdatemodDateT);
                    System.out.println("added");
                    addstmt.executeUpdate();
                    updateDateT.executeUpdate();
                }
            }
            rs = stmt.executeQuery();
            boolean hasnext = rs.next();
            conn.commit();

            while (hasnext) {
                rturn = rturn +
                        "<p>" + rs.getString("task") + "</p>\n" +
                        "<form action=\"/demo/ahihi/todolist/update\" method=\"post\" enctype=\"application/x-www-form-urlencoded\" >\n"
                        +
                        "<input type=\"text\" id=\"updatetask\" name=\"update\" path=\"uptask\" >\n" +
                        "<input type=\"hidden\" name=\"oldtask\" value=\"" + rs.getString("task")
                        + "\" path=\"oldtask\">\n" +
                        "<input type=\"submit\" value=\"update\">\n" +
                        "</form>\n" +
                        "<form action=\"/demo/ahihi/todolist/delete/" + rs.getString("task")
                        + "\" method=\"get\" >\n" +
                        "<input type=\"submit\" value=\"delete\">\n" +
                        "</form><br>";
                hasnext = rs.next();
            }
            rturn += "\n</body>\n</html>";

        } catch (SQLException se) {
            try {
                if (conn != null)
                    conn.rollback();
            } catch (SQLException se2) {
                se2.printStackTrace();
            }

        } catch (Exception E) {
            E.printStackTrace();
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException SE3) {
                    SE3.printStackTrace();
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException SE3) {
                    SE3.printStackTrace();
                }
            }
        }
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("Content-Type", "text/html; charset=utf-8");
        return new ResponseEntity<String>(rturn, responseHeaders, HttpStatus.CREATED);

    }
}

final class updatetask implements Callable<String> {
    private String oldtask = "";
    private String newtask = "";

    public updatetask(String oldtask, String newtask) {
        this.oldtask = oldtask;
        this.newtask = newtask;
    }

    @Override
    public String call() throws Exception {

        if (oldtask == null || newtask == null)
            return null;
        if (oldtask.isEmpty() || newtask.isEmpty())
            return null;
        final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
        final String DB_URL = "jdbc:mysql://localhost:3306/todolist?useUnicode=yes&character_set_server=utf8mb4";
        final String USER = "authenticationuser";
        final String PASS = "436553";
        final String sqlupdate = "UPDATE  tdlist SET task=N'" + newtask + "' WHERE task LIKE N'" + oldtask
                + "'";
        LocalDateTime now = LocalDateTime.now();
        ZonedDateTime zonedDateTime = now.atZone(ZoneId.of("GMT"));
        long millisec = zonedDateTime.toInstant().toEpochMilli();
        final String sqlupdatemodDateT = "update  lastmodified set DateT=" + millisec;
        Connection conn = null;
        PreparedStatement updatestmt = null;
        PreparedStatement updateDateT = null;
        try {
            Class.forName(JDBC_DRIVER);
            conn = DriverManager.getConnection(DB_URL, USER, PASS);
            conn.setAutoCommit(false);
            updatestmt = conn.prepareStatement(sqlupdate);
            updateDateT = conn.prepareStatement(sqlupdatemodDateT);
            updatestmt.execute();
            updateDateT.execute();
            conn.commit();

        } catch (SQLException se) {

            try {
                if (conn != null)
                    conn.rollback();
            } catch (SQLException se2) {
                se2.printStackTrace();
            }

        } catch (Exception E) {
            E.printStackTrace();
        } finally {
            if (updatestmt != null) {
                try {
                    updatestmt.close();
                } catch (SQLException SE3) {
                    SE3.printStackTrace();
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException SE4) {
                    SE4.printStackTrace();
                }
            }
        }
        return null;
    }
}

final class deletetask implements Callable<String> {
    private String deletetask;

    public deletetask(String deletetask) {
        this.deletetask = deletetask;
    }

    @Override
    public String call() throws Exception {

        if (deletetask == null)
            return null;
        if (deletetask.isEmpty())
            return null;

        final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
        final String DB_URL = "jdbc:mysql://localhost:3306/todolist?useUnicode=yes&characterEncoding=UTF-8";
        final String USER = "authenticationuser";
        final String PASS = "436553";
        final String sqlupdate = "DELETE FROM  tdlist WHERE task='" + deletetask + "'";
        LocalDateTime now = LocalDateTime.now();
        ZonedDateTime zonedDateTime = now.atZone(ZoneId.of("GMT"));
        long millisec = zonedDateTime.toInstant().toEpochMilli();
        final String sqlupdatemodDateT = "update  lastmodified set DateT=" + millisec;

        Connection conn = null;
        PreparedStatement updatestmt = null;
        PreparedStatement updateDateT = null;
        try {
            Class.forName(JDBC_DRIVER);
            conn = DriverManager.getConnection(DB_URL, USER, PASS);
            conn.setAutoCommit(false);
            updatestmt = conn.prepareStatement(sqlupdate);
            updateDateT = conn.prepareStatement(sqlupdatemodDateT);
            updateDateT.execute();
            updatestmt.executeUpdate();
            conn.commit();
        } catch (SQLException se) {
            try {
                if (conn != null)
                    conn.rollback();
            } catch (SQLException se2) {
                se2.printStackTrace();
            }

        } catch (Exception E) {
            E.printStackTrace();
        } finally {
            if (updatestmt != null) {
                try {
                    updatestmt.close();
                } catch (SQLException SE3) {
                    SE3.printStackTrace();
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException SE3) {
                    SE3.printStackTrace();
                }
            }
        }
        return null;
    }
}