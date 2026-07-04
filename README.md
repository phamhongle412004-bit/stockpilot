# StockPilot — Hệ Thống Quản Lý Kho Hàng & Xử Lý Đơn Hàng

StockPilot là một giải pháp backend toàn diện chạy trên giao diện dòng lệnh (CLI Console), được thiết kế cho một công ty phân phối để quản lý sản phẩm, xử lý đơn hàng nhất quán và theo dõi số liệu phân tích thời gian thực. Được xây dựng trên kiến trúc phân tầng sạch sẽ bằng Java Core SE, hệ thống loại bỏ các sai sót từ việc quản lý bằng bảng tính truyền thống, ngăn chặn tình trạng bán quá số lượng kho (overselling) trong các đợt Flash Sale có tải cao và cung cấp các báo cáo doanh thu chuyên sâu.

---

## Công Nghệ Sử Dụng
* **Ngôn ngữ:** Java 17
* **Công cụ quản lý dự án & Build:** Maven
* **Cơ sở dữ liệu:** H2 Database
* **Thư viện kiểm thử:** JUnit 5

---

## Mô Tả Cấu Trúc Bảng Database (Schema)
Cơ sở dữ liệu quan hệ bao gồm 4 bảng chính được ràng buộc chặt chẽ bằng Khóa ngoại (Foreign Key) và các điều kiện ràng buộc dữ liệu (Constraints):

1.  **`products`**: Lưu trữ thông tin hàng hóa và tồn kho.
    * `id` (INT, Khóa chính, Tự động tăng)
    * `sku` (VARCHAR, Duy nhất, khớp với định dạng regex `^[A-Z]{3}-\d{4}$` hoặc cấu hình hệ thống)
    * `name` (VARCHAR, Tên sản phẩm)
    * `category` (VARCHAR, Danh mục sản phẩm)
    * `price` (DECIMAL, Sử dụng kiểu dữ liệu này để đảm bảo độ chính xác tuyệt đối trong tính toán tiền tệ)
    * `stock_quantity` (INT, Ràng buộc kiểm tra: `>= 0` nhằm ngăn chặn kho bị âm)

2.  **`customers`**: Lưu trữ thông tin khách hàng bán lẻ.
    * `id` (INT, Khóa chính, Tự động tăng)
    * `name`, `email`, `phone` (VARCHAR, Được xác thực định dạng nghiêm ngặt qua Regex)

3.  **`orders`**: Theo dõi các giao dịch mua hàng.
    * `id` (INT, Khóa chính, Tự động tăng)
    * `customer_id` (INT, Khóa ngoại liên kết tới `customers(id)`)
    * `order_date` (TIMESTAMP, Thời gian đặt hàng)
    * `total_amount`, `discount_amount`, `final_amount` (DECIMAL, Các trường lưu trữ số tiền)

4.  **`order_items`**: Chi tiết từng mặt hàng trong đơn hàng (Giải quyết mối quan hệ Nhiều-Nhiều giữa Đơn hàng và Sản phẩm).
    * `id` (INT, Khóa chính, Tự động tăng)
    * `order_id` (INT, Khóa ngoại liên kết tới `orders(id)`)
    * `product_id` (INT, Khóa ngoại liên kết tới `products(id)`)
    * `quantity` (INT, Số lượng mua)
    * `price` (DECIMAL, Giá tại thời điểm mua)

---

## BÁO CÁO PHÂN TÍCH TRANH CHẤP ĐA LUỒNG (FLASH SALE WRITE-UP)

### 1. Hiện tượng Tranh Chấp Dữ Liệu (Kịch bản UNSAFE)
Trong một sự kiện có lượng truy cập lớn như Flash Sale, hệ thống nhận đồng thời rất nhiều yêu cầu đặt hàng tại cùng một mili-giây từ các luồng (threads) khác nhau cho cùng một sản phẩm có số lượng giới hạn (Ví dụ: sản phẩm `SP004` chỉ còn đúng 10 mặt hàng). Nếu không có cơ chế điều phối luồng thích hợp, hiện tượng **Tranh chấp dữ liệu (Race Condition)** sẽ xảy ra trong tiến trình kiểm tra và trừ kho (Check-and-Decrement).

Nhiều luồng sẽ cùng truy cập vào Database tại cùng một thời điểm và đều đọc ra kết quả là `stock_quantity > 0` (vẫn còn hàng). Do đó, tất cả các luồng này đều vượt qua điều kiện kiểm tra một cách hợp lệ, tiến hành tạo đơn hàng và thực hiện câu lệnh trừ kho dưới DB. Hệ quả là sản phẩm bị bán lố âm vượt quá số lượng tồn kho thực tế (**Oversold**), làm sai lệch dữ liệu kho nghiêm trọng và gây thất thoát cho doanh nghiệp.

### 2. Giải pháp Khắc Phục (Kịch bản SAFE)
Để giải quyết triệt để vấn đề này, hệ thống áp dụng cơ chế khóa tiến trình tập trung ngay bên trong tầng logic `OrderService` bằng một khối đồng bộ hóa tĩnh:
* **`synchronized (CLASS_LOCK)`**: Do các luồng được cấp phát rất nhanh và có thể bỏ qua các cơ chế khóa ở cấp độ instance (đối tượng), một khóa tĩnh toàn cục (static object lock) được triển khai để ép toàn bộ các luồng phải "xếp hàng" tuần tự ngay tại điểm bắt đầu của logic đặt hàng.
* **Cơ chế hoạt động**: Tại một thời điểm, chỉ có duy nhất một luồng giành được khóa để đi vào bên trong. Luồng này sẽ mở kết nối, đọc dữ liệu tồn kho mới nhất trực tiếp từ DB, thực hiện trừ kho, commit Transaction một cách cô lập hoàn toàn rồi mới giải phóng khóa cho luồng tiếp theo.
* **Kết quả**: Đúng 10 yêu cầu đặt hàng đầu tiên đến hệ thống sẽ chốt đơn thành công và xuất hóa đơn. Từ luồng thứ 11 trở đi, sau khi xếp hàng và được vào trong, hệ thống sẽ đọc thấy số lượng tồn kho mới đã cập nhật về `0`, lập tức chặn đứng lại một cách an toàn bằng cách ném ra ngoại lệ `InsufficientStockException`. Kho hàng được bảo vệ tuyệt đối, không xảy ra hiện tượng bán quá số lượng có sẵn.

---

##  Hướng Dẫn Cài Đặt và Khởi Chạy Ứng Dụng

### 1. Biên dịch và Đóng gói
Mở cửa sổ dòng lệnh (Terminal/PowerShell) tại thư mục gốc của dự án và chạy câu lệnh Maven sau để biên dịch code và nén thành file JAR:
```bash
mvn clean package
```

### 2. Khởi chạy ứng dụng
Sau khi quá trình build hoàn tất thành công, khởi chạy file ứng dụng sản phẩm nằm trong thư mục `target/` bằng lệnh:
```bash
java -jar target/stockpilot-1.0.0.jar
```

---

## Danh Sách Checklist Tính Năng

### Phân hạng Đạt
* Cấu hình dự án Maven hoàn chỉnh, quản lý thư viện tập trung và đóng gói ra file JAR chạy độc lập.
* Áp dụng tư duy hướng đối tượng (OOP) để mô hình hóa các thực thể cốt lõi: Product, Customer, Order, OrderItem.
* Xây dựng tầng kết nối dữ liệu Persistent bằng JDBC thuần, sử dụng PreparedStatement để tối ưu câu lệnh SQL và đóng mở tài nguyên an toàn.
* Triển khai quy trình đặt hàng tiêu chuẩn (checkout): Kiểm tra sự tồn tại của sản phẩm, kiểm tra số lượng tồn kho và cập nhật trừ kho chính xác dưới Database.
* Thiết kế hệ thống ngoại lệ tùy chỉnh (ProductNotFoundException, InsufficientStockException) giúp phân loại và bắt lỗi tập trung tại tầng CLI.
* Triển khai kiến trúc mẫu thiết kế generic Repository<T, ID> giúp chuẩn hóa các thao tác CRUD dữ liệu.

### Phân hạng Khá/Giỏi
* Tiến trình đặt hàng hoạt động dưới dạng một Transaction nhất quán (commit khi thành công, rollback khi lỗi).
* Toàn bộ các báo cáo thống kê doanh thu, top sản phẩm, danh mục đều xử lý bằng Java Stream API (groupingBy, lọc, sắp xếp).
* Đọc và bóc tách dữ liệu thông minh từ file CSV; kết xuất dữ liệu hóa đơn/báo cáo gọn gàng ra thư mục `output/`.
* Sử dụng biểu thức Lambda để triển khai các functional interface tự định nghĩa `@FunctionalInterface PricingRule` và các bộ so sánh `Comparator`.

### Phân hạng Xuất sắc
* Giả lập kịch bản Flash Sale đa luồng đồng thời kết hợp cơ chế đồng bộ hóa tĩnh ngăn chặn hoàn toàn việc bán lố kho.
* Viết bài giải trình kỹ thuật chi tiết (Write-Up) làm rõ lỗi tranh chấp luồng và giải pháp xử lý bằng khóa.
* Xây dựng bộ kiểm thử JUnit 5 Unit Test Suite (OrderServiceTest) bao phủ các kịch bản logic nghiệp vụ quan trọng và kiểm tra các ngưỡng chặn ném ngoại lệ (assertThrows).
* Tổ chức cấu trúc mã nguồn theo kiến trúc phân tầng tách biệt (Layered Architecture): Tách biệt rõ ràng giữa giao diện dòng lệnh (CLI), Tầng xử lý nghiệp vụ (Services) và Tầng tương tác cơ sở dữ liệu (Repositories).
* Sử dụng kiểu dữ liệu `BigDecimal` cho toàn bộ các thuộc tính liên quan đến tiền tệ, đảm bảo độ chính xác tuyệt đối, tránh sai số làm tròn trong tính toán tài chính.

