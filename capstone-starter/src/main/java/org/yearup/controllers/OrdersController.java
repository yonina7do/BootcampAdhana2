package org.yearup.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.yearup.data.OrderDao;
import org.yearup.data.ProfileDao;
import org.yearup.data.ShoppingCartDao;
import org.yearup.data.UserDao;
import org.yearup.models.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;

@RestController
@RequestMapping("orders")
@CrossOrigin
@PreAuthorize("isAuthenticated()")
public class OrdersController
{
    private OrderDao orderDao;
    private ShoppingCartDao shoppingCartDao;
    private UserDao userDao;
    private ProfileDao profileDao;

    @Autowired
    public OrdersController(OrderDao orderDao, ShoppingCartDao shoppingCartDao, UserDao userDao, ProfileDao profileDao)
    {
        this.orderDao = orderDao;
        this.shoppingCartDao = shoppingCartDao;
        this.userDao = userDao;
        this.profileDao = profileDao;
    }

    @PostMapping("")
    public Order checkout(Principal principal)
    {
        try
        {
            String userName = principal.getName();
            User user = userDao.getByUserName(userName);
            int userId = user.getId();

            ShoppingCart cart = shoppingCartDao.getByUserId(userId);

            if(cart.getItems().isEmpty())
            {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart is empty");
            }

            Profile profile = profileDao.getByUserId(userId);

            Order order = new Order();
            order.setUserId(userId);
            order.setDate(LocalDateTime.now());
            order.setAddress(profile.getAddress());
            order.setCity(profile.getCity());
            order.setState(profile.getState());
            order.setZip(profile.getZip());
            order.setShippingAmount(new BigDecimal("0"));

            order = orderDao.create(order);

            for(ShoppingCartItem cartItem : cart.getItems().values())
            {
                OrderLineItem lineItem = new OrderLineItem();
                lineItem.setOrderId(order.getOrderId());
                lineItem.setProductId(cartItem.getProductId());
                lineItem.setSalesPrice(cartItem.getProduct().getPrice());
                lineItem.setQuantity(cartItem.getQuantity());
                lineItem.setDiscount(cartItem.getDiscountPercent());

                orderDao.addOrderLineItem(lineItem);
            }

            shoppingCartDao.clearCart(userId);

            return order;
        }
        catch(ResponseStatusException e)
        {
            throw e;
        }
        catch(Exception e)
        {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Oops... our bad.");
        }
    }
}