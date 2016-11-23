package com.example.loading;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;

/**
 * Created by aning on 16-6-1.
 */

public class indexController {
    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String loading() throws IOException, XMLStreamException {

        return "index";

    }
}
