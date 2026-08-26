package com.tidsec.novaeyetech_backend.config;

import com.tidsec.novaeyetech_backend.dto.QuotationItemRequest;
import com.tidsec.novaeyetech_backend.dto.QuotationRequest;
import com.tidsec.novaeyetech_backend.model.Client;
import com.tidsec.novaeyetech_backend.model.Product;
import com.tidsec.novaeyetech_backend.model.ProductCategory;
import com.tidsec.novaeyetech_backend.model.ServiceCategory;
import com.tidsec.novaeyetech_backend.model.ServiceItem;
import com.tidsec.novaeyetech_backend.model.Supplier;
import com.tidsec.novaeyetech_backend.model.User;
import com.tidsec.novaeyetech_backend.model.enums.QuotationItemType;
import com.tidsec.novaeyetech_backend.model.enums.Role;
import com.tidsec.novaeyetech_backend.repo.IClientRepo;
import com.tidsec.novaeyetech_backend.repo.IProductCategoryRepo;
import com.tidsec.novaeyetech_backend.repo.IProductRepo;
import com.tidsec.novaeyetech_backend.repo.IServiceCategoryRepo;
import com.tidsec.novaeyetech_backend.repo.IServiceItemRepo;
import com.tidsec.novaeyetech_backend.repo.ISupplierRepo;
import com.tidsec.novaeyetech_backend.repo.IUserRepo;
import com.tidsec.novaeyetech_backend.security.AuthenticatedUser;
import com.tidsec.novaeyetech_backend.service.IQuotationService;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Siembra de datos de demostracion.
 *
 * <p>Solo corre con {@code app.seed.enabled=true} y solo si la tabla de usuarios esta vacia: nunca
 * pisa datos existentes. Sustituye al script {@code npm run seed} del backend NestJS.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DataSeeder implements CommandLineRunner {

    private static final String DEMO_PASSWORD = "Admin123*";

    private final IUserRepo userRepo;
    private final IClientRepo clientRepo;
    private final ISupplierRepo supplierRepo;
    private final IProductCategoryRepo productCategoryRepo;
    private final IProductRepo productRepo;
    private final IServiceCategoryRepo serviceCategoryRepo;
    private final IServiceItemRepo serviceItemRepo;
    private final IQuotationService quotationService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepo.count() > 0) {
            log.info("La base ya tiene usuarios: se omite la siembra de datos de demostracion.");
            return;
        }

        User admin = seedUsers();
        Client client = seedClients();
        Supplier supplier = seedSupplier();
        Product product = seedProducts(supplier);
        ServiceItem service = seedServices();
        seedQuotation(admin, client, product, service);

        log.info("Datos de demostracion creados. Usuario: admin1@nexteye.com / {}", DEMO_PASSWORD);
    }

    private User seedUsers() {
        String hash = passwordEncoder.encode(DEMO_PASSWORD);

        User admin = userRepo.save(User.builder()
                .email("admin1@nexteye.com")
                .fullName("Administrador Principal")
                .password(hash)
                .role(Role.ADMINISTRADOR)
                .phone("0990000001")
                .active(true)
                .build());

        userRepo.save(User.builder()
                .email("tecnico1@nexteye.com")
                .fullName("Tecnico Carlos Mena")
                .password(hash)
                .role(Role.TECNICO)
                .phone("0981000001")
                .active(true)
                .build());

        return admin;
    }

    private Client seedClients() {
        return clientRepo.save(Client.builder()
                .nameOrBusinessName("Comercial Andina S.A.")
                .documentNumber("1790012345001")
                .phone("022345678")
                .email("contacto@comercialandina.com")
                .address("Av. Amazonas N34-100")
                .city("Quito")
                .commercialReference("Referido por camara de comercio")
                .active(true)
                .build());
    }

    private Supplier seedSupplier() {
        return supplierRepo.save(Supplier.builder()
                .businessName("Distribuidora SegurTec")
                .ruc("1791234567001")
                .contact("Maria Lopez")
                .phone("023456789")
                .email("ventas@segurtec.com")
                .address("Av. Republica E7-20")
                .city("Quito")
                .active(true)
                .build());
    }

    private Product seedProducts(Supplier supplier) {
        ProductCategory category = productCategoryRepo.save(ProductCategory.builder()
                .name("Camaras de seguridad")
                .description("Camaras IP y analogicas")
                .active(true)
                .build());

        return productRepo.save(Product.builder()
                .category(category)
                .mainSupplier(supplier)
                .internalCode("CAM-DOME-001")
                .name("Camara domo IP 4MP")
                .brand("Hikvision")
                .model("DS-2CD1143G0")
                .description("Camara domo IP 4MP con vision nocturna e IR de 30 metros")
                .baseCost(new BigDecimal("85.00"))
                .stock(new BigDecimal("25.00"))
                .unit("unidad")
                .active(true)
                .build());
    }

    private ServiceItem seedServices() {
        ServiceCategory category = serviceCategoryRepo.save(ServiceCategory.builder()
                .name("Instalacion")
                .description("Servicios de instalacion en sitio")
                .active(true)
                .build());

        return serviceItemRepo.save(ServiceItem.builder()
                .category(category)
                .name("Instalacion de camara")
                .description("Instalacion, configuracion y puesta en marcha por punto")
                .baseCost(new BigDecimal("35.00"))
                .active(true)
                .build());
    }

    /**
     * La cotizacion se crea por el servicio y no por el repositorio, para que quede con la misma
     * numeracion, los mismos calculos y el mismo registro de auditoria que una real.
     */
    private void seedQuotation(User author, Client client, Product product, ServiceItem service) {
        QuotationItemRequest productItem = new QuotationItemRequest();
        productItem.setItemType(QuotationItemType.PRODUCTO);
        productItem.setProductId(product.getId());
        productItem.setQuantity(new BigDecimal("4"));

        QuotationItemRequest serviceItem = new QuotationItemRequest();
        serviceItem.setItemType(QuotationItemType.SERVICIO);
        serviceItem.setServiceId(service.getId());
        serviceItem.setQuantity(new BigDecimal("4"));
        // Margen 0: la instalacion se cotiza a precio cerrado.
        serviceItem.setMarginPercent(BigDecimal.ZERO);

        QuotationRequest request = new QuotationRequest();
        request.setClientId(client.getId());
        request.setObservations("Cotizacion de demostracion generada por la siembra de datos.");
        request.setItems(List.of(productItem, serviceItem));

        quotationService.create(request,
                new AuthenticatedUser(author.getId(), author.getEmail(), author.getRole()));
    }
}
