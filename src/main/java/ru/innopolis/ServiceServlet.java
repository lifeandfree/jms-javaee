package ru.innopolis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.jms.Destination;
import javax.jms.JMSContext;
import javax.jms.JMSDestinationDefinition;
import javax.jms.JMSDestinationDefinitions;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;

@JMSDestinationDefinitions(
        value = {
                @JMSDestinationDefinition(
                        name = "java:/jms/queue/INNOQueue",
                        interfaceName = "javax.jms.Queue",
                        destinationName = "InnoQueue"
                ),
                @JMSDestinationDefinition(
                        name = "java:/jms/topic/INNOTopic",
                        interfaceName = "javax.jms.Topic",
                        destinationName = "InnoTopic"
                )
        }
)
@WebServlet(name = "service", urlPatterns = {"/service"}, loadOnStartup = 1)
public class ServiceServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(QueueReceiver.class);
    @Inject
    private JMSContext context;

    @Resource(lookup = "java:/jms/queue/INNOQueue")
    private Queue queue;

    @Resource(lookup = "java:/jms/topic/INNOTopic")
    private Topic topic;

    public ServiceServlet() {
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("text/html;charcet=UTF-8");
        boolean viewBrowser = request.getParameterMap().containsKey("browser");
        if (viewBrowser) {
            viewBrowser(request, response);
        } else {
            viewService(request, response);
        }
    }

    private void viewService(HttpServletRequest request, HttpServletResponse response) throws IOException {
        boolean useTopic = request.getParameterMap().containsKey("topic");
        final Destination destination = useTopic ? topic : queue;
        try (PrintWriter out = response.getWriter()) {
            out.print("<html><body>");
            out.print("<h1>" + destination.toString() + "</h1>");
            for (int i = 0; i < 4; i++) {
                String text = "Message #" + i;
                out.print("<li>" + text);
                final TextMessage textMessage = context.createTextMessage(text);
                textMessage.setStringProperty("JMSExpireTest", String.valueOf(i % 2 == 0));
                context.createProducer()
                        .setTimeToLive(3000)
                        .send(destination, textMessage);
            }
            out.print("</body></html>");
        } catch (JMSException e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void viewBrowser(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try (PrintWriter out = response.getWriter()) {
            out.print("<html><body>");
            final QueueBrowser browser = context.createBrowser(queue);
            final Enumeration messageEnumeration = browser.getEnumeration();
            while (messageEnumeration.hasMoreElements()) {
                out.print("<li>" + messageEnumeration.nextElement());
            }
            out.print("</body></html>");
        } catch (JMSException e) {
            logger.error(e.getMessage(), e);
        }
    }
}