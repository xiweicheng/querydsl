package com.mysema.util;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

public final class ClassPathUtils {
    
    private static final Pattern JAR_URL_SEPARATOR = Pattern.compile("!");
    
    public static Set<Class<?>> scanPackage(ClassLoader classLoader, Package pkg) throws IOException {
        Enumeration<URL> urls = classLoader.getResources(pkg.getName().replace('.', '/'));
        Set<Class<?>> classes = new HashSet<Class<?>>();
        while (urls.hasMoreElements()){
            URL url = urls.nextElement();
            if (url.getProtocol().equals("jar")){
                scanJar(classes, url);

            }else if (url.getProtocol().equals("file")){
                scanDirectory(pkg, classes, url);

            }else{
                throw new IllegalArgumentException("Illegal url : " + url);
            }
        }
        return classes;
    }

    private static void scanDirectory(Package pkg, Set<Class<?>> classes, URL url) throws IOException {
        Deque<File> files = new ArrayDeque<File>();
        String packagePath;
        try {
            File packageAsFile = new File(url.toURI());
            packagePath = packageAsFile.getPath();
            files.add(packageAsFile);
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
        while (!files.isEmpty()){
            File file = files.pop();
            for (File child : file.listFiles()){
                if (child.getName().endsWith(".class")){
                    String fileName = child.getPath().substring(packagePath.length()+1).replace(File.separatorChar, '.');
                    String className = pkg.getName() + "." + fileName.substring(0, fileName.length()-6);
                    Class<?> cl = safeClassForName(className);
                    if (cl != null) {
                        classes.add(cl);
                    }
                }else if (child.isDirectory()){
                    files.add(child);
                }
            }
        }
    }

    private static void scanJar(Set<Class<?>> classes, URL url) throws IOException {
        String[] fileAndPath = JAR_URL_SEPARATOR.split(url.getFile().substring(5));
        JarFile jarFile = new JarFile(fileAndPath[0]);
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()){
            JarEntry entry = entries.nextElement();
            if (entry.getName().endsWith(".class") && entry.getName().startsWith(fileAndPath[1].substring(1))){
                String className = entry.getName().substring(0, entry.getName().length()-6).replace('/', '.');
                Class<?> cl = safeClassForName(className);
                if (cl != null) {
                    classes.add(cl);
                }
            }
        }
    }
    
    private static Class<?> safeClassForName(String className){
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private ClassPathUtils() {}
    
}