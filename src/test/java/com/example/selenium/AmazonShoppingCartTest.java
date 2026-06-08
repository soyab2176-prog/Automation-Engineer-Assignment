package com.example.selenium;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.extension.TestWatcher;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

@Execution(ExecutionMode.CONCURRENT)
public class AmazonShoppingCartTest {

    private static final Path REPORT_FOLDER = Paths.get("target", "extent-report");
    private static final Path SCREENSHOT_FOLDER = REPORT_FOLDER.resolve("screenshots");
    private static ExtentReports extent;
    private static final ThreadLocal<ExtentTest> extentTest = new ThreadLocal<>();

    private WebDriver driver;
    private WebDriverWait wait;

    @RegisterExtension
    TestWatcher watcher = new TestWatcher() {
        @Override
        public void testFailed(ExtensionContext context, Throwable cause) {
            String screenshotPath = captureScreenshot(context.getDisplayName());
            log("Test failed: " + cause.getMessage());
            if (screenshotPath != null) {
                getExtentTest().fail(cause).addScreenCaptureFromPath(screenshotPath);
            } else {
                getExtentTest().fail(cause);
            }
        }

        @Override
        public void testSuccessful(ExtensionContext context) {
            getExtentTest().pass("Test passed");
        }
    };

    @BeforeAll
    static void setupDriver() throws Exception {
        WebDriverManager.chromedriver().setup();
        initExtentReport();
    }

    @AfterAll
    static void flushReport() {
        if (extent != null) {
            extent.flush();
        }
    }

    @BeforeEach
    void openBrowser(TestInfo testInfo) {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--start-maximized");
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(5));

        ExtentTest test = extent.createTest(testInfo.getDisplayName());
        test.assignCategory("Amazon Shopping Cart");
        extentTest.set(test);
    }

    @AfterEach
    void closeBrowser() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    void addIphoneToCart() {
        runAmazonShoppingFlow("iPhone 16");
    }

    @Test
    void addGalaxyToCart() {
        runAmazonShoppingFlow("Galaxy S25");
    }

    private void runAmazonShoppingFlow(String searchTerm) {
        log("Navigating to Amazon.com");
        driver.get("https://www.amazon.in/");
        acceptCookiesIfVisible();

        log("Searching for " + searchTerm);
        String productPrice = searchAndAddFirstAvailableProduct(searchTerm);
        log("Selected product price: " + productPrice);
        System.out.println(searchTerm + " product price: " + productPrice);

        log("Opening cart page");
        clickCartIcon();

        String cartPrice = getCartPrice();
        log("Cart total price: " + cartPrice);
        System.out.println(searchTerm + " cart price: " + cartPrice);

        assertFalse(productPrice.isBlank(), "Product price should not be blank.");
        assertFalse(cartPrice.isBlank(), "Cart price should not be blank.");
    }

    private static void initExtentReport() throws Exception {
        Files.createDirectories(REPORT_FOLDER);
        Files.createDirectories(SCREENSHOT_FOLDER);

        Path reportFile = REPORT_FOLDER.resolve("index.html");
        ExtentSparkReporter spark = new ExtentSparkReporter(reportFile.toFile());
        spark.config().setDocumentTitle("Amazon Shopping Cart Report");
        spark.config().setReportName("Amazon Shopping Cart Automation");
        spark.config().setTheme(com.aventstack.extentreports.reporter.configuration.Theme.STANDARD);

        extent = new ExtentReports();
        extent.attachReporter(spark);
        extent.setSystemInfo("Environment", "Local");
        extent.setSystemInfo("Browser", "Chrome");
    }

    private String searchAndAddFirstAvailableProduct(String searchTerm) {
        WebElement searchBox = wait.until(ExpectedConditions.elementToBeClickable(By.id("twotabsearchtextbox")));
        searchBox.clear();
        searchBox.sendKeys(searchTerm);
        searchBox.submit();
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("div[data-component-type='s-search-result']")));

        List<WebElement> searchResults = driver.findElements(By.cssSelector("div[data-component-type='s-search-result']"));
        for (WebElement result : searchResults) {
            String price = getPriceFromResultCard(result);
            if (price.isBlank()) {
                continue;
            }
            WebElement addToCartButton = findAddToCartButtonInResultCard(result);
            if (addToCartButton == null) {
                continue;
            }
            String title = getTitleFromResultCard(result);
            log("Adding product from results: " + title + " -> " + price);
            scrollTo(addToCartButton);
            clickElement(addToCartButton);
            waitForAddToCartConfirmation();
            return price;
        }

        throw new IllegalStateException("Could not find a valid search result item with a direct Add to Cart button.");
    }

    private void acceptCookiesIfVisible() {
        List<By> cookieSelectors = List.of(
                By.id("sp-cc-accept"),
                By.xpath("//input[@name='accept']"),
                By.xpath("//button[contains(., 'Accept all') or contains(., 'I agree') or contains(., 'Agree')]")
        );

        for (By selector : cookieSelectors) {
            try {
                WebElement button = wait.until(ExpectedConditions.elementToBeClickable(selector));
                button.click();
                log("Accepted cookie prompt.");
                return;
            } catch (Exception ignored) {
                // Try next cookie selector.
            }
        }
    }

    private String getPriceFromResultCard(WebElement resultCard) {
        List<By> priceSelectors = List.of(
                By.cssSelector("span.a-price > span.a-offscreen"),
                By.cssSelector("span.a-price-whole"),
                By.cssSelector("span.a-price-fraction"),
                By.cssSelector("span.a-offscreen")
        );

        for (By selector : priceSelectors) {
            try {
                WebElement element = resultCard.findElement(selector);
                    String text = element.getText().trim();
                    if (!text.isBlank()) {
                        if (selector.equals(By.cssSelector("span.a-price-whole"))) {
                            String fraction = findTextInElement(resultCard, By.cssSelector("span.a-price-fraction"));
                            return normalizePrice(text + (fraction.isBlank() ? "" : "." + fraction));
                        }
                        return normalizePrice(text);
                    }
            } catch (Exception ignored) {
                // continue scanning selectors.
            }
        }

        return "";
    }

    private String findTextInElement(WebElement parent, By selector) {
        try {
            WebElement element = parent.findElement(selector);
            return normalizePrice(element.getText().trim());
        } catch (Exception ignored) {
            return "";
        }
    }

    /**
     * Normalize price text to a consistent format and fix common mojibake sequences.
     * Examples: "Γé╣74,999.00" -> "₹74,999.00"
     */
    private String normalizePrice(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.replace('\u00A0', ' ').trim();
        // Map common mojibake sequences back to the rupee symbol
        s = s.replace("Γé╣", "₹");
        s = s.replace("â‚¹", "₹");
        // Collapse whitespace
        s = s.replaceAll("\\s+", " ").trim();
        // Keep only digits, comma, dot, rupee symbol and minus sign
        s = s.replaceAll("[^0-9,\\.₹\\-]", "");
        return s;
    }

    private WebElement findAddToCartButtonInResultCard(WebElement resultCard) {
        List<By> selectors = List.of(
                By.cssSelector("button[name='submit.addToCart']"),
                By.cssSelector("button[name='submit.add-to-cart']"),
                By.cssSelector("button[aria-label*='Add to cart']"),
                By.cssSelector("input[name='submit.addToCart']"),
                By.cssSelector("input[name='submit.add-to-cart']")
        );

        for (By selector : selectors) {
            try {
                List<WebElement> buttons = resultCard.findElements(selector);
                for (WebElement button : buttons) {
                    if (button.isDisplayed() && button.isEnabled()) {
                        return button;
                    }
                }
            } catch (Exception ignored) {
                // continue with next selector
            }
        }
        return null;
    }

    private String getTitleFromResultCard(WebElement resultCard) {
        try {
            WebElement titleElement = resultCard.findElement(By.cssSelector("h2 a span"));
            return titleElement.getText().trim();
        } catch (Exception ignored) {
            return "Unknown product";
        }
    }

    private void waitForAddToCartConfirmation() {
        wait.until(ExpectedConditions.or(
                ExpectedConditions.visibilityOfElementLocated(By.id("nav-cart-count")),
                ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#attach-added-to-cart-message")),
                ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#sw-atc-details-single-container"))
        ));
    }

    private void clickCartIcon() {
        WebElement cartIcon = wait.until(ExpectedConditions.elementToBeClickable(By.id("nav-cart")));
        cartIcon.click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("span.sc-price")));
    }

    private String getCartPrice() {
        return findFirstText(List.of(
                By.cssSelector("span.a-size-medium.a-color-base.sc-price.sc-white-space-nowrap"),
                By.cssSelector("span.sc-price")
        ));
    }

    private String findFirstText(List<By> selectors) {
        for (By selector : selectors) {
            try {
                WebElement element = wait.until(ExpectedConditions.visibilityOfElementLocated(selector));
                String text = element.getText().trim();
                if (!text.isBlank()) {
                    return normalizePrice(text);
                }
            } catch (Exception ignored) {
                // try next selector
            }
        }
        return "";
    }

    private void clickElement(WebElement element) {
        try {
            element.click();
        } catch (Exception e) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
        }
    }

    private void scrollTo(WebElement element) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", element);
    }

    private String captureScreenshot(String testName) {
        try {
            if (driver == null) {
                return null;
            }
            Files.createDirectories(SCREENSHOT_FOLDER);
            File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
                String safeName = testName.replaceAll("[^a-zA-Z0-9_\\-\\. ]", "_");
            Path targetPath = SCREENSHOT_FOLDER.resolve(safeName + "_" + System.currentTimeMillis() + ".png");
            Files.copy(screenshot.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            return targetPath.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private void log(String message) {
        getExtentTest().info(message);
    }

    private ExtentTest getExtentTest() {
        return extentTest.get();
    }
}