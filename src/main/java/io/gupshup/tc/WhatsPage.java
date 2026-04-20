package io.gupshup.tc;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitForSelectorState;

import java.nio.file.Paths;

import static io.gupshup.tc.constans.Constants.*;

public class WhatsPage {

    private final Page page;
    private final Browser browser;
    private final Playwright playwright;

    Page.WaitForSelectorOptions oneSecondWait =  new Page.WaitForSelectorOptions()
                                .setTimeout(2000).setState(WaitForSelectorState.ATTACHED);

    WhatsPage(){
        playwright = Playwright.create();

        browser = playwright.chromium().launch(
        new BrowserType.LaunchOptions().setHeadless(false)
        );

        BrowserContext context = playwright.chromium().launchPersistentContext(
        Paths.get("user-data"),
        new BrowserType.LaunchPersistentContextOptions()
                .setHeadless(false)
        );

        page = context.newPage();
        page.navigate("https://web.whatsapp.com");

        System.out.println("Scan QR if not logged in...");

        //page.waitForSelector("canvas", new Page.WaitForSelectorOptions().setTimeout(60000));
        page.waitForSelector(SEARCH_BOX,
                new Page.WaitForSelectorOptions().setTimeout(0));

        context.storageState(new BrowserContext.StorageStateOptions()
                .setPath(Paths.get("auth.json")));

    }

    public void sendMessage(String number, String message, String file){
        if(file == null || file.isBlank()){
            sendTextMessage(number, message, true);
            return;
        }
        sendTextMessage(number, message, false);
        attachFile(file);
    }

    public void sendMessage(String number, String message){
            sendTextMessage(number, message, true);
    }

    private void sendTextMessage(String number, String message, boolean pressEnter){

        clickNewChat(number);
        page.waitForTimeout(1000);
        typeText(MESSAGE_BOX, message, pressEnter);
    }

    private void attachFile(String file){
        page.waitForTimeout(1000);
        clickElement("span[data-icon='plus-rounded']");
        FileChooser fileChooser = page.waitForFileChooser(
                () -> page.click("button[aria-label='"+getFileType(file)+"']"));
        fileChooser.setFiles(Paths.get(file));
       clickElement(FILE_SEND_BUTTON);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void closeBrowser(){
        if(browser != null && browser.isConnected()){
            browser.close();
            System.out.println("Browser Closed");
            playwright.close();
        }

    }

    private void clickNewChat(String number){
        clickElement(NEW_CHAT_BTN);
        fillText(NEW_CHAT, number);
    }

    private void fillText(String selector, String message) {
        page.waitForSelector(selector);
        page.fill(selector,message);
        page.waitForTimeout(1000);
        boolean err = false;
        try{
            page.waitForSelector(NEW_CHAT_IMG,
                    oneSecondWait);
            clickElement(NEW_CHAT_IMG);

        }catch (Exception e){
            err = true;
            throw new RuntimeException("Number not found: " + message);
        }finally {
            if(err){
                clickElement(BACK_BUTTON);
            }
        }
//        page.keyboard().press("Enter");
        System.out.println("pressed Enter");

    }

    private void typeText(String selector, String message, Boolean pressEnter){
        clickElement(selector);
        page.keyboard().type(message);

        if(pressEnter)
            page.keyboard().press("Enter");
    }

    private void clickElement(String selector){
        page.waitForSelector(selector);
        page.click(selector);

    }

    private String getFileType(String file) {
        String ext;
        int lastDot = file.lastIndexOf('.');
        ext = (lastDot == -1)?"":file.substring(lastDot + 1).toLowerCase();

        return switch (ext) {
            case "png", "jpg", "jpeg", "mp4" -> "Photos & videos";
            default -> "DOCUMENT";
        };
    }
}
