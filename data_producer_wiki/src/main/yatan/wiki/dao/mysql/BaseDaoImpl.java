package yatan.wiki.dao.mysql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import yatan.wiki.dao.BaseDao;
import yatan.wiki.entities.BaseEntity;

public abstract class BaseDaoImpl<T extends BaseEntity> implements BaseDao<T> {
    private Connection connection;

    protected static void close(ResultSet rs, PreparedStatement ps) {
        try {
            if (rs != null) {
                rs.close();
            }
        } catch (SQLException e) {
            // ignore
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException e) {
                // ignore
            }
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }
}
