package dev.bandarlog.graph;

import java.util.Map;

import org.apache.tinkerpop.gremlin.driver.message.RequestMessage;
import org.apache.tinkerpop.gremlin.process.traversal.Bytecode;
import org.apache.tinkerpop.gremlin.server.auth.AuthenticatedUser;
import org.apache.tinkerpop.gremlin.server.authz.AuthorizationException;
import org.apache.tinkerpop.gremlin.server.authz.Authorizer;

public class OPAAuthorizer implements Authorizer {

    @Override
    public void authorize(AuthenticatedUser user, RequestMessage msg) throws AuthorizationException {
        System.out.println("OPAAuthorizer.authorize(" + user + ", " + msg + ");");
        new Throwable().printStackTrace();
    }

    @Override
    public Bytecode authorize(AuthenticatedUser user, Bytecode bytecode, Map<String, String> aliases)
            throws AuthorizationException {
        System.out.println("OPAAuthorizer.authorize(" + user + ", " + bytecode + ", " + aliases + ");");
        new Throwable().printStackTrace();
        return bytecode;
    }

    @Override
    public void setup(Map<String, Object> config) throws AuthorizationException {
        System.out.println("OPAAuthorizer.setup(" + config + ");");
        new Throwable().printStackTrace();
    }
}