package top.integer.app.dao;

import top.integer.framework.core.ioc.annotation.Inject;
import top.integer.framework.core.ioc.annotation.Repository;

import javax.sql.DataSource;
import java.sql.SQLException;

@Repository
public class FirstDao {

    DataSource dataSource;

    public FirstDao(DataSource dataSource) throws SQLException {
        this.dataSource = dataSource;
    }

    public DataSource getDataSource() {
        return dataSource;
    }
}
