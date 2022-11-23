package top.integer.app.service;

import top.integer.app.dao.FirstDao;
import top.integer.framework.core.ioc.annotation.Inject;
import top.integer.framework.core.ioc.annotation.Service;

@Service
public class FirstService {
    @Inject
    FirstDao dao;



    public FirstDao getDao() {
        return dao;
    }
}
