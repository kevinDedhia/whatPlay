package io.gupshup.tc;

import com.microsoft.playwright.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class WhatsAppSender {

    private final Map<String, Integer> stats = new HashMap<>();
    WhatsPage whatsPage = new WhatsPage();

    private static class Contact {
        String name;
        String number;

        Contact(String name, String number) {
            this.name = name.replace("\"","");
            this.number = number.replace("\"","");
        }
    }

    public static void main(String[] args) throws IOException {
        WhatsAppSender whatsAppSender = new WhatsAppSender();
        whatsAppSender.sendMessageToCustomers("message with replacement {{name}}", "/Users/kevin.dedhia/IdeaProjects/whatPlay/src/main/resources/test.csv", "/Users/kevin.dedhia/Downloads/Test1.jpeg");

    }

    public void sendMessageToCustomers(String message, String excel, String file) throws IOException {

        List<Contact> contacts = readCSV(excel);
        List<String> errors = new ArrayList<>();
        stats.put("success", 0);
        stats.put("error", 0);
        stats.put("completed", 0);
        stats.put("total", contacts.size());

        for(Contact contact : contacts){
            try {
                whatsPage.sendMessage(contact.number,
                        message.replace("{{name}}",
                                contact.name), file);

                stats.merge("success", 1, Integer::sum);

            }catch (Exception e){
                System.out.println(e.getMessage());
                errors.add(e.getMessage());
                stats.merge("error", 1, Integer::sum);
            }finally {
                stats.merge("completed", 1, Integer::sum);
            }

        }

        System.out.println(errors);

    }

    public Map<String, Integer> getStats(){
       return stats;
    }

    private List<Contact> readCSV(String filePath) throws IOException {
        List<Contact> list = new ArrayList<>();

        List<String> lines = Files.readAllLines(Paths.get(filePath));

        for (int i = 1; i < lines.size(); i++) {
            String[] parts = lines.get(i).split(",");
            list.add(new Contact(parts[0].trim(), parts[1].trim()));
        }

        return list;
    }

}