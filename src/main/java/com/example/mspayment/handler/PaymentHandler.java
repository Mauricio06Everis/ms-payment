package com.example.mspayment.handler;

import com.example.mspayment.models.entities.Acquisition;
import com.example.mspayment.models.entities.Payment;
import com.example.mspayment.services.AcquisitionService;
import com.example.mspayment.services.IPaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.net.URI;

@Component
@Slf4j(topic = "PAYMENT_HANDLER")
public class PaymentHandler {
    private final IPaymentService paymentService;
    private final AcquisitionService acquisitionService;

    @Autowired
    public PaymentHandler(IPaymentService paymentService, AcquisitionService acquisitionService) {
        this.paymentService = paymentService;
        this.acquisitionService = acquisitionService;
    }


    public Mono<ServerResponse> findAll(ServerRequest request){
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                .body(paymentService.findAll(), Payment.class);
    }

    public Mono<ServerResponse> findById(ServerRequest request) {
        String id = request.pathVariable("id");
        return paymentService.findById(id).flatMap(p -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(p))
                .switchIfEmpty(Mono.error(new RuntimeException("Payment no found")));
    }

    public Mono<ServerResponse> findByAcquisitionCardNumber(ServerRequest request){
        String cardNumber = request.pathVariable("cardNumber");
        return acquisitionService.findByCardNumber(cardNumber).flatMap(p -> ServerResponse.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(p))
                        .switchIfEmpty(ServerResponse.notFound().build());
    }

    public Mono<ServerResponse> updateAcquisition(ServerRequest request){
        Mono<Acquisition> acquisition = request.bodyToMono(Acquisition.class);
        String cardNumber = request.pathVariable("cardNumber");
        return acquisition.flatMap(acquisition1 -> acquisitionService.updateAcquisition(acquisition1, cardNumber)).flatMap(p -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(p))
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    public Mono<ServerResponse> save(ServerRequest request){
        Mono<Payment> payment = request.bodyToMono(Payment.class);
        return payment.flatMap(paymentService::create)
                .flatMap(p -> ServerResponse.created(URI.create("/payment/".concat(p.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(p));
    }

    public Mono<ServerResponse> update(ServerRequest request){
        Mono<Payment> payment = request.bodyToMono(Payment.class);
        String id = request.pathVariable("id");
        Mono<Payment> paymentDB = paymentService.findById(id);
        return paymentDB.zipWith(payment, (db, req) -> {
           db.setAmount(req.getAmount());
           db.setDescription(req.getDescription());
           return db;
        }).flatMap(paymentService::update).flatMap(p -> ServerResponse.created(URI.create("/payment/".concat(p.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(p));
    }
}
