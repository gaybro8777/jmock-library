/* Copyright (c) 2000-2003, jMock.org. See LICENSE.txt */
package org.jmock.dynamic;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import junit.framework.AssertionFailedError;

import org.jmock.Constraint;
import org.jmock.constraint.IsAnything;
import org.jmock.dynamic.matcher.ArgumentsMatcher;
import org.jmock.dynamic.matcher.NoArgumentsMatcher;
import org.jmock.dynamic.stub.CustomStub;
import org.jmock.dynamic.stub.ReturnStub;


public class CoreMock 
	implements DynamicMock 
{
    private InvocationDispatcher invocationDispatcher;
    private Object proxy;
    private String name;

    public CoreMock(Class mockedClass, String name, InvocationDispatcher invocationDispatcher ) {
        this.proxy = Proxy.newProxyInstance( getClass().getClassLoader(), 
                                             new Class[]{mockedClass}, 
                                             this);
        this.name = name;
        this.invocationDispatcher = invocationDispatcher;
        
        setupDefaultBehaviour();
    }
    
    public Object proxy() {
        return this.proxy;
    }
    
    public Object invoke(Object invokedProxy, Method method, Object[] args)
            throws Throwable 
    {
        Invocation invocation = new Invocation(method, args);
        try {
            return invocationDispatcher.dispatch(invocation);
        }
        catch (AssertionFailedError failure) {
            DynamicMockError mockFailure = 
            	new DynamicMockError(this, invocation, invocationDispatcher, failure.getMessage());
            
			mockFailure.fillInStackTrace();
			throw mockFailure;
        }
    }
    
    public void verify() {
        try {
            invocationDispatcher.verify();
        } catch (AssertionFailedError ex) {
            throw new AssertionFailedError(name + ": " + ex.getMessage());
        }
    }
    
    public String toString() {
        return this.name;
    }
    
    public String getMockName() {
        return this.name;
    }
    
	public void setDefaultStub(Stub newDefaultStub) {
		invocationDispatcher.setDefaultStub(newDefaultStub);
	}
	
    public void add(Invokable invokable) {
        invocationDispatcher.add(invokable);
    }
    
    public void reset() {
        //TODO write tests for this
        invocationDispatcher.clear();
        setupDefaultBehaviour();
    }
    
    public static String mockNameFromClass(Class c) {
        return "mock" + DynamicUtil.classShortName(c);
    }
    
    private void setupDefaultBehaviour() {
        add(new SilentInvocationMocker("toString", NoArgumentsMatcher.INSTANCE, new ReturnStub(this.name)));
        add(new SilentInvocationMocker("equals", 
                new ArgumentsMatcher(new Constraint[] {new IsAnything()}), 
                new IsSameAsProxy(this.proxy)));
    }

    private static class SilentInvocationMocker extends InvocationMocker {
        public SilentInvocationMocker(String methodName, InvocationMatcher arguments, Stub stub) {
            super(methodName, arguments, stub);
        }

        public StringBuffer writeTo(StringBuffer buffer) {
            return buffer;
        }
    }
    
    private static class IsSameAsProxy extends CustomStub {
        private Object proxyRef;
        
        private IsSameAsProxy(Object proxyRef) {
            super("returns whether equal to proxy");
            this.proxyRef = proxyRef;
        }
        
        public Object invoke( Invocation invocation ) throws Throwable {
            return new Boolean(invocation.getParameterValues().get(0) == proxyRef);
        }
    }
}